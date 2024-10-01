/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cstdio>
#include <cstdlib>
#include <limits>
#include <string.h>
#include <string>

#include "KAssert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "KString.Latin1.h"
#include "KString.UTF16.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include "polyhash/PolyHash.h"
#include "polyhash/naive.h"

using namespace kotlin;

namespace {

static constexpr const uint32_t MAX_STRING_SIZE =
    static_cast<uint32_t>(std::numeric_limits<int32_t>::max());

/*
 * The interface is as follows:
 *   class EncodingAwareString {
 *     typename unit;
 *     Iterator begin();
 *     Iterator end();
 *     Iterator at(const UnitType*); // in variable-length encodings, rewinds to the last valid unit boundary
 *     size_t sizeInUnits(); // in the smallest units of this encoding, e.g. 32-bit integers for UTF-32 and 8-bit ones for UTF-8
 *     size_t sizeInChars(); // in UTF-16 equivalent, counting surrogate pairs as 2 characters; may not be constant-time
 *     static bool canEncode(KChar);
 *     static OBJ_GETTER(createUninitialized, size_t sizeInUnits);
 *   }
 *   class Iterator : BidirectionalIterator<KChar> {
 *     const UnitType* ptr(); // not well-defined if in the middle of a surrogate pair
 *     Iterator operator+(size_t); // may not be constant-time
 *     size_t operator-(Iterator); // may not be constant-time
 *   }
 * See `KString.*.h` for implementations.
 */
template <typename F>
auto encodingAware(KConstRef string, F&& impl) {
    auto header = StringHeaderOf(string);
    auto data = StringRawData(string);
    auto size = StringRawSize(string, header && header->ignoreLastByte());
    switch (header ? header->encoding() : StringHeader::ENCODING_UTF16) {
    case StringHeader::ENCODING_UTF16:
        return impl(UTF16String{reinterpret_cast<const KChar*>(data), size / sizeof(KChar)});
    case StringHeader::ENCODING_LATIN1:
        return impl(Latin1String{reinterpret_cast<const uint8_t*>(data), size});
    default: ThrowIllegalArgumentException();
    }
}

void setLatin1Flags(KRef string, size_t lengthBytes) {
    const_cast<StringHeader*>(StringHeaderOf(string))->flags_ =
        (StringHeader::ENCODING_LATIN1 << StringHeader::ENCODING_OFFSET) |
        (lengthBytes % 2 ? StringHeader::IGNORE_LAST_BYTE : 0);
}

bool utf8StringIsASCII(const char* utf8, size_t lengthBytes) {
    // TODO: there are easy vectorized ways to do this check REALLY FAST, will std::all_of use them?..
    return std::all_of(utf8, utf8 + lengthBytes, [](char c) { return c >= 0; });
}

template <typename String1, typename String2>
constexpr bool isSameEncoding(String1&& a, String2&& b) {
    return std::is_same_v<std::decay_t<String1>, std::decay_t<String2>>;
}

template <typename String, typename It>
bool isInSurrogatePair(String&& string, It&& it) {
    return string.at(it.ptr()) != it;
}

template <typename String, typename F /*= void(UnitType*) */>
OBJ_GETTER(createString, uint32_t lengthUnits, F&& initializer) {
    if (lengthUnits == 0) RETURN_RESULT_OF0(TheEmptyString);
    auto result = String::createUninitialized(lengthUnits, OBJ_RESULT);
    initializer(reinterpret_cast<typename String::unit*>(StringRawData(result)));
    return result;
}

template <typename String, typename F /*= void(KChar*) */>
OBJ_GETTER(createWithEncodingOf, String&& other, uint32_t lengthUnits, F&& initializer) {
    RETURN_RESULT_OF(createString<std::decay_t<String>>, lengthUnits, std::forward<F>(initializer));
}

OBJ_GETTER(createStringFromUTF8, const char* utf8, uint32_t lengthBytes, bool ensureValid) {
    if (utf8 == nullptr) RETURN_OBJ(nullptr);
    if (lengthBytes == 0) RETURN_RESULT_OF0(TheEmptyString);
    if (utf8StringIsASCII(utf8, lengthBytes)) {
        RETURN_RESULT_OF(createString<Latin1String>, lengthBytes, [=](uint8_t* out) { std::copy_n(utf8, lengthBytes, out); })
    }
    size_t lengthChars;
    try {
        lengthChars = ensureValid
            ? utf8::utf16_length(utf8, utf8 + lengthBytes)
            : utf8::with_replacement::utf16_length(utf8, utf8 + lengthBytes);
    } catch (...) {
        ThrowCharacterCodingException();
    }
    RETURN_RESULT_OF(createString<UTF16String>, lengthChars, [=](KChar* out) {
        return ensureValid
            ? utf8::unchecked::utf8to16(utf8, utf8 + lengthBytes, out) // already known to be valid
            : utf8::with_replacement::utf8to16(utf8, utf8 + lengthBytes, out);
    });
}

OBJ_GETTER(unsafeConvertToUTF8, KConstRef thiz, KStringConversionMode mode, KInt start, KInt size) {
    RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must use String");
    std::string utf8 = kotlin::to_string(thiz, mode, static_cast<size_t>(start), static_cast<size_t>(size));
    auto result = AllocArrayInstance(theByteArrayTypeInfo, utf8.size(), OBJ_RESULT);
    std::copy(utf8.begin(), utf8.end(), ByteArrayAddressOfElementAt(result->array(), 0));
    return result;
}

const char* unsafeGetByteArrayData(KConstRef thiz, KInt start) {
    RuntimeAssert(thiz->type_info() == theByteArrayTypeInfo, "Must use a byte array");
    return reinterpret_cast<const char*>(ByteArrayAddressOfElementAt(thiz->array(), start));
}

template <typename T>
PERFORMANCE_INLINE inline auto boundsCheckedIteratorAt(T string, KInt index) {
    // We couldn't have created a string bigger than max KInt value.
    // So if index is < 0, conversion to an unsigned value would make it bigger
    // than the array size.
    if (static_cast<uint32_t>(index) >= string.sizeInChars()) {
        ThrowArrayIndexOutOfBoundsException();
    }
    return string.begin() + index;
}

} // namespace

extern "C" OBJ_GETTER(CreateStringFromCString, const char* cstring) {
    RETURN_RESULT_OF(CreateStringFromUtf8, cstring, cstring ? strlen(cstring) : 0);
}

extern "C" OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes) {
    RETURN_RESULT_OF(createStringFromUTF8, utf8, lengthBytes, false);
}

extern "C" OBJ_GETTER(CreateStringFromUtf8OrThrow, const char* utf8, uint32_t lengthBytes) {
    RETURN_RESULT_OF(createStringFromUTF8, utf8, lengthBytes, true);
}

extern "C" OBJ_GETTER(CreateStringFromUtf16, const KChar* utf16, uint32_t lengthChars) {
    if (utf16 == nullptr) RETURN_OBJ(nullptr);
    RETURN_RESULT_OF(createString<UTF16String>, lengthChars, [=](KChar* out) { std::copy_n(utf16, lengthChars, out); });
}

extern "C" OBJ_GETTER(CreateUninitializedUtf16String, uint32_t lengthChars) {
    if (lengthChars == 0) RETURN_RESULT_OF0(TheEmptyString);
    RETURN_RESULT_OF(AllocArrayInstance, theStringTypeInfo, lengthChars + (lengthChars > STRING_HEADER_SIZE ? STRING_HEADER_SIZE : 0));
}

extern "C" OBJ_GETTER(CreateUninitializedLatin1String, uint32_t lengthBytes) {
    if (lengthBytes == 0) RETURN_RESULT_OF0(TheEmptyString);
    auto result = AllocArrayInstance(theStringTypeInfo, (lengthBytes + 1) / 2 + STRING_HEADER_SIZE, OBJ_RESULT);
    setLatin1Flags(result, lengthBytes);
    return result;
}

extern "C" char* CreateCStringFromString(KConstRef kref) {
    if (kref == nullptr) return nullptr;
    std::string utf8 = to_string(kref);
    char* result = reinterpret_cast<char*>(std::calloc(1, utf8.size() + 1));
    std::copy(utf8.begin(), utf8.end(), result);
    return result;
}

extern "C" void DisposeCString(char* cstring) {
    if (cstring) std::free(cstring);
}

static KRef allocatePermanentString(size_t sizeInChars) {
    size_t headerSize = alignUp(sizeof(ArrayHeader), alignof(KChar));
    size_t arraySize = headerSize + sizeInChars * sizeof(KChar);
    auto obj = reinterpret_cast<ObjHeader*>(std::calloc(arraySize, 1));
    obj->typeInfoOrMeta_ = setPointerBits((TypeInfo *)theStringTypeInfo, OBJECT_TAG_PERMANENT_CONTAINER);
    obj->array()->count_ = sizeInChars;
    return obj;
}

extern "C" KRef CreatePermanentStringFromCString(const char* nullTerminatedUTF8) {
    // Note: this function can be called in "Native" thread state. But this is fine:
    //   while it indeed manipulates Kotlin objects, it doesn't in fact access _Kotlin heap_,
    //   because the accessed object is off-heap, imitating permanent static objects.
    auto sizeInBytes = strlen(nullTerminatedUTF8);
    if (sizeInBytes > 0 && utf8StringIsASCII(nullTerminatedUTF8, sizeInBytes)) {
        auto result = allocatePermanentString((sizeInBytes + 1) / 2 + STRING_HEADER_SIZE);
        setLatin1Flags(result, sizeInBytes);
        std::copy_n(nullTerminatedUTF8, sizeInBytes, StringRawData(result));
        return result;
    } else {
        auto end = nullTerminatedUTF8 + sizeInBytes;
        auto sizeInChars = utf8::with_replacement::utf16_length(nullTerminatedUTF8, end);
        auto result = allocatePermanentString(sizeInChars + (sizeInChars > STRING_HEADER_SIZE ? STRING_HEADER_SIZE : 0));
        utf8::with_replacement::utf8to16(nullTerminatedUTF8, end, reinterpret_cast<KChar*>(StringRawData(result)));
        return result;
    }
}

extern "C" void FreePermanentStringForTests(KConstRef header) {
    std::free(const_cast<KRef>(header));
}

// String.kt
extern "C" KInt Kotlin_String_getStringLength(KConstRef thiz) {
    return encodingAware(thiz, [](auto thiz) { return thiz.sizeInChars(); });
}

extern "C" OBJ_GETTER(Kotlin_String_replace, KConstRef thizPtr, KChar oldChar, KChar newChar) {
    return encodingAware(thizPtr, [=](auto thiz) {
        if (!thiz.canEncode(oldChar)) RETURN_OBJ(const_cast<KRef>(thizPtr));
        if (isLatin1(thiz) && thiz.canEncode(newChar)) {
            RETURN_RESULT_OF(createString<Latin1String>, thiz.sizeInUnits(),
                [=](uint8_t* out) { std::replace_copy(thiz.begin().ptr(), thiz.end().ptr(), out, oldChar, newChar); })
        }
        RETURN_RESULT_OF(createString<UTF16String>, thiz.sizeInChars(),
            [=](KChar* out) { std::replace_copy(thiz.begin(), thiz.end(), out, oldChar, newChar); });
    });
}

extern "C" OBJ_GETTER(Kotlin_String_plusImpl, KConstRef thiz, KConstRef other) {
    RuntimeAssert(thiz != nullptr, "this cannot be null");
    RuntimeAssert(other != nullptr, "other cannot be null");
    RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must be a string");
    RuntimeAssert(other->type_info() == theStringTypeInfo, "Must be a string");
    if (thiz->array()->count_ == 0) RETURN_OBJ(const_cast<KRef>(other));
    if (other->array()->count_ == 0) RETURN_OBJ(const_cast<KRef>(thiz));
    return encodingAware(thiz, [=](auto thiz) {
        return encodingAware(other, [=](auto other) {
            RuntimeAssert(thiz.sizeInChars() <= MAX_STRING_SIZE, "this cannot be this large");
            RuntimeAssert(other.sizeInChars() <= MAX_STRING_SIZE, "other cannot be this large");
            auto resultLength = thiz.sizeInChars() + other.sizeInChars(); // can't overflow since MAX_STRING_SIZE is (max value)/2
            if (resultLength > MAX_STRING_SIZE) {
                ThrowOutOfMemoryError();
            }

            if (isSameEncoding(thiz, other) &&
                // In non-UTF-16 encodings, the total size in units could still overflow, e.g.
                // UTF-8 has characters that encode to 3 bytes while only needing 2 in UTF-16.
                (isUTF16(thiz) || thiz.sizeInUnits() < std::numeric_limits<size_t>::max() - other.sizeInUnits())
            ) {
                RETURN_RESULT_OF(createWithEncodingOf, thiz, thiz.sizeInUnits() + other.sizeInUnits(), [=](auto* out) {
                    auto halfway = std::copy(thiz.begin().ptr(), thiz.end().ptr(), out);
                    std::copy(other.begin().ptr(), other.end().ptr(), halfway);
                });
            } else {
                RETURN_RESULT_OF(createString<UTF16String>, thiz.sizeInChars() + other.sizeInChars(), [=](KChar* out) {
                    auto halfway = std::copy(thiz.begin(), thiz.end(), out);
                    std::copy(other.begin(), other.end(), halfway);
                });
            }
        });
    });
}

extern "C" OBJ_GETTER(Kotlin_String_unsafeStringFromCharArray, KConstRef thiz, KInt start, KInt size) {
    RuntimeAssert(thiz->type_info() == theCharArrayTypeInfo, "Must use a char array");
    RETURN_RESULT_OF(createString<UTF16String>, size,
        [=](KChar* out) { std::copy_n(CharArrayAddressOfElementAt(thiz->array(), start), size, out); });
}

static void Kotlin_String_overwriteArray(KConstRef string, KRef destination, KInt destinationOffset, KInt start, KInt size) {
    encodingAware(string, [=](auto string) {
        auto it = string.begin() + start;
        auto out = CharArrayAddressOfElementAt(destination->array(), destinationOffset);
        if constexpr (isUTF16(string)) {
            std::copy_n(it.ptr(), size, out);
        } else {
            std::copy_n(it, size, out);
        }
    });
}

extern "C" OBJ_GETTER(Kotlin_String_toCharArray, KConstRef string, KRef destination, KInt destinationOffset, KInt start, KInt size) {
    Kotlin_String_overwriteArray(string, destination, destinationOffset, start, size);
    RETURN_OBJ(destination);
}

extern "C" OBJ_GETTER(Kotlin_String_subSequence, KConstRef thiz, KInt startIndex, KInt endIndex) {
    return encodingAware(thiz, [=](auto thiz) {
        if (startIndex < 0 || static_cast<uint32_t>(endIndex) > thiz.sizeInChars() || startIndex > endIndex) {
            // TODO: is it correct exception?
            ThrowArrayIndexOutOfBoundsException();
        }

        if (startIndex == endIndex) {
            RETURN_RESULT_OF0(TheEmptyString);
        }

        auto start = thiz.begin() + startIndex;
        auto end = start + (endIndex - startIndex);
        if (isInSurrogatePair(thiz, start) || isInSurrogatePair(thiz, end)) {
            RETURN_RESULT_OF(createString<UTF16String>, endIndex - startIndex,
                [=](KChar* out) { std::copy(start, end, out); });
        }
        RETURN_RESULT_OF(createWithEncodingOf, thiz, end.ptr() - start.ptr(),
            [=](auto* out) { std::copy(start.ptr(), end.ptr(), out); });
    });
}

template <typename It1, typename It2>
static KInt Kotlin_String_compareAt(It1 it1, It1 end1, It2 it2, It2 end2) {
    if (it1 == end1 && it2 == end2) return 0;
    if (it1 == end1) return -1;
    if (it2 == end2) return 1;
    KChar c1 = *it1, c2 = *it2;
    if (c1 == c2) {
        // Assuming the iterators were produced by std::mismatch, this is only possible
        // when searching in raw memory then rolling back to the previous unit in non-UTF-16
        // encodings. In this case this must be a surrogate pair where the first element is
        // equal, but the second element is not.
        c1 = *++it1;
        c2 = *++it2;
    }
    return c1 < c2 ? -1 : 1;
}

extern "C" KInt Kotlin_String_compareTo(KConstRef thiz, KConstRef other) {
    return encodingAware(thiz, [=](auto thiz) {
        return encodingAware(other, [=](auto other) {
            auto begin1 = thiz.begin(), end1 = thiz.end();
            auto begin2 = other.begin(), end2 = other.end();
            if constexpr (isSameEncoding(thiz, other)) {
                auto [ptr1, ptr2] = std::mismatch(begin1.ptr(), end1.ptr(), begin2.ptr(), end2.ptr());
                return Kotlin_String_compareAt(thiz.at(ptr1), end1, other.at(ptr2), end2);
            } else {
                auto [it1, it2] = std::mismatch(begin1, end1, begin2, end2);
                return Kotlin_String_compareAt(it1, end1, it2, end2);
            }
        });
    });
}

extern "C" KChar Kotlin_String_get(KConstRef thiz, KInt index) {
    return encodingAware(thiz, [=](auto thiz) { return *boundsCheckedIteratorAt(thiz, index); });
}

extern "C" OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8OrThrow, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(CreateStringFromUtf8OrThrow, unsafeGetByteArrayData(thiz, start), size);
}

extern "C" OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(CreateStringFromUtf8, unsafeGetByteArrayData(thiz, start), size);
}

extern "C" OBJ_GETTER(Kotlin_String_unsafeStringToUtf8, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(unsafeConvertToUTF8, thiz, KStringConversionMode::REPLACE_INVALID, start, size);
}

extern "C" OBJ_GETTER(Kotlin_String_unsafeStringToUtf8OrThrow, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(unsafeConvertToUTF8, thiz, KStringConversionMode::CHECKED, start, size);
}

extern "C" KInt Kotlin_StringBuilder_insertString(KRef builder, KInt distIndex, KConstRef fromString, KInt sourceIndex, KInt count) {
    Kotlin_String_overwriteArray(fromString, builder, distIndex, sourceIndex, count);
    return count;
}

extern "C" KInt Kotlin_StringBuilder_insertInt(KRef builder, KInt position, KInt value) {
    auto toArray = builder->array();
    RuntimeAssert(toArray->count_ >= static_cast<uint32_t>(11 + position), "must be true");
    char cstring[12];
    auto length = std::snprintf(cstring, sizeof(cstring), "%d", value);
    RuntimeAssert(length >= 0, "This should never happen"); // may be overkill
    RuntimeAssert(static_cast<size_t>(length) < sizeof(cstring), "Unexpectedly large value"); // Can't be, but this is what sNprintf for
    auto* from = &cstring[0];
    auto* to = CharArrayAddressOfElementAt(toArray, position);
    while (*from) {
        *to++ = *from++;
    }
    return from - cstring;
}

extern "C" KBoolean Kotlin_String_equals(KConstRef thiz, KConstRef other) {
    if (other == nullptr || other->type_info() != theStringTypeInfo) return false;
    // TODO: if hash code is computed and unequal, then strings are also unequal
    return thiz == other || encodingAware(thiz, [=](auto thiz) {
        return encodingAware(other, [=](auto other) {
            if constexpr (isSameEncoding(thiz, other)) {
                return std::equal(thiz.begin().ptr(), thiz.end().ptr(), other.begin().ptr(), other.end().ptr());
            } else {
                return std::equal(thiz.begin(), thiz.end(), other.begin(), other.end());
            }
        });
    });
}

// Bounds checks is are performed on Kotlin side
extern "C" KBoolean Kotlin_String_unsafeRangeEquals(KConstRef thiz, KInt thizOffset, KConstRef other, KInt otherOffset, KInt length) {
    return length == 0 || encodingAware(thiz, [=](auto thiz) {
        return encodingAware(other, [=](auto other) {
            auto begin1 = thiz.begin() + thizOffset;
            auto begin2 = other.begin() + otherOffset;
            // Questionable moment: in variable-length encodings, is it more efficient to advance the iterator first
            // and then compare the known fixed range, or to decode characters one by one and count while comparing?
            auto end1 = begin1 + length;
            auto end2 = begin2 + length;
            if constexpr (!isSameEncoding(thiz, other)) {
                return std::equal(begin1, end1, begin2, end2);
            }
            // Assuming only one "canonical" encoding, can byte-compare encoded values.
            // Since ptr() is only well-defined at unit boundaries, surrogates at ends should be checked separately.
            bool startsWithUnequalLowSurrogate = isInSurrogatePair(thiz, begin1)
                ? !isInSurrogatePair(other, begin2) || *begin1++ != *begin2++ // safe because length != 0
                : isInSurrogatePair(other, begin2);
            if (startsWithUnequalLowSurrogate) return false;
            bool endsWithUnequalHighSurrogate = isInSurrogatePair(thiz, end1)
                ? !isInSurrogatePair(other, end2) || *--end1 != *--end2 // safe because begin1 and begin2 are not in a surrogate pair
                : isInSurrogatePair(other, end2);
            if (endsWithUnequalHighSurrogate) return false;
            return std::equal(begin1.ptr(), end1.ptr(), begin2.ptr(), end2.ptr());
        });
    });
}

extern "C" KBoolean Kotlin_Char_isISOControl(KChar ch) {
    return (ch <= 0x1F) || (ch >= 0x7F && ch <= 0x9F);
}

extern "C" KBoolean Kotlin_Char_isHighSurrogate(KChar ch) {
    return ((ch & 0xfc00) == 0xd800);
}

extern "C" KBoolean Kotlin_Char_isLowSurrogate(KChar ch) {
    return ((ch & 0xfc00) == 0xdc00);
}

extern "C" KInt Kotlin_String_indexOfChar(KConstRef thiz, KChar ch, KInt fromIndex) {
    auto unsignedIndex = fromIndex < 0 ? 0 : static_cast<size_t>(fromIndex);
    return encodingAware(thiz, [=](auto thiz) {
        auto i = std::min(unsignedIndex, thiz.sizeInChars());
        for (auto it = thiz.begin() + i; i < thiz.sizeInChars(); ++i) {
            if (*it++ == ch) return static_cast<KInt>(i);
        }
        return -1;
    });
}

extern "C" KInt Kotlin_String_lastIndexOfChar(KConstRef thiz, KChar ch, KInt fromIndex) {
    if (fromIndex < 0) return -1;
    auto unsignedIndex = static_cast<size_t>(fromIndex) + 1; // convert to exclusive bound
    return encodingAware(thiz, [=](auto thiz) {
        auto i = std::min(unsignedIndex, thiz.sizeInChars());
        for (auto it = thiz.begin() + i; i-- > 0; ) {
            if (*--it == ch) return static_cast<KInt>(i);
        }
        return -1;
    });
}

// TODO: or code up Knuth-Moris-Pratt, or use std::boyer_moore_searcher (might need backporting)
extern "C" KInt Kotlin_String_indexOfString(KConstRef thiz, KConstRef other, KInt fromIndex) {
    auto unsignedIndex = fromIndex < 0 ? 0 : static_cast<size_t>(fromIndex);
    return encodingAware(thiz, [=](auto thiz) {
        return encodingAware(other, [=](auto other) {
            auto thizLength = thiz.sizeInChars();
            auto otherLength = other.sizeInChars();
            if (unsignedIndex >= thizLength) {
                return otherLength == 0 ? static_cast<KInt>(thizLength) : -1;
            } else if (otherLength > thizLength) {
                return -1;
            } else if (otherLength == 0) {
                return static_cast<KInt>(unsignedIndex);
            }

            auto start = thiz.begin() + unsignedIndex, end = thiz.end();
            auto patternStart = other.begin(), patternEnd = other.end();
            if constexpr (isSameEncoding(thiz, other)) {
                auto shift = unsignedIndex;
                while (start != end) {
                    if (isInSurrogatePair(thiz, start)) {
                        // `start` points into a surrogate pair, skip its second half since presumably
                        // this encoding doesn't allow `other` to start with it anyway.
                        ++start;
                        ++shift;
                    }
                    auto ptr = std::search(start.ptr(), end.ptr(), patternStart.ptr(), patternEnd.ptr());
                    if (ptr == end.ptr()) break;
                    auto it = thiz.at(ptr);
                    if (ptr == it.ptr()) return static_cast<KInt>(it - start + shift);
                    // Found a bytewise match, but it starts in the middle of a unit, so it's not a character-wise match.
                    shift += it - start + 1;
                    start = ++it;
                }
                return -1;
            } else {
                auto it = std::search(start, end, patternStart, patternEnd);
                return it == end ? -1 : static_cast<KInt>(it - start + unsignedIndex);
            }
        });
    });
}

// TODO: this is basically equivalent to a pure Kotlin version...is there a faster way to implement this?
extern "C" KInt Kotlin_String_lastIndexOfString(KConstRef thiz, KConstRef other, KInt fromIndex) {
    KInt count = Kotlin_String_getStringLength(thiz);
    KInt otherCount = Kotlin_String_getStringLength(other);

    if (fromIndex < 0 || otherCount > count) {
        return -1;
    }
    if (otherCount == 0) {
        return fromIndex < count ? fromIndex : count;
    }

    KInt start = std::min(fromIndex, count - otherCount);
    KChar firstChar = Kotlin_String_get(other, 0);
    while (true) {
        KInt candidate = Kotlin_String_lastIndexOfChar(thiz, firstChar, start);
        if (candidate == -1) return -1;
        if (Kotlin_String_unsafeRangeEquals(thiz, candidate, other, 0, otherCount)) return candidate;
        start = candidate - 1;
    }
}

extern "C" KInt Kotlin_String_hashCode(KConstRef thiz) {
    int32_t flags = 0;
    auto header = StringHeaderOf(thiz);
    if (header != nullptr) {
        flags = kotlin::std_support::atomic_ref{header->flags_}.load(std::memory_order_acquire);
        if (flags & StringHeader::HASHCODE_COMPUTED) {
            // The condition only enforces an ordering with the first thread to write the hash code,
            // so if two thread concurrently computed the hash, an atomic read is needed to prevent a data race.
            // The value is always the same, though, so which write is observed is irrelevant.
            return kotlin::std_support::atomic_ref{header->hashCode_}.load(std::memory_order_relaxed);
        }
    }
    KInt result = encodingAware(thiz, [](auto thiz) {
        if constexpr (isUTF16(thiz)) {
            return polyHash(thiz.sizeInUnits(), thiz.begin().ptr());
        } else {
            // TODO: faster specific implementations?..
            return polyHash_naive(thiz.begin(), thiz.end());
        }
    });
    if (header != nullptr) {
        StringHeader* nonConst = const_cast<StringHeader*>(header);
        kotlin::std_support::atomic_ref{nonConst->hashCode_}.store(result, std::memory_order_relaxed);
        // TODO: use fetch_or once atomic_ref has it; for now this is fine since this is the only mutable flag.
        kotlin::std_support::atomic_ref{nonConst->flags_}.store(flags | StringHeader::HASHCODE_COMPUTED, std::memory_order_release);
    }
    return result;
}

static void Kotlin_String_ensureUTF16(KConstRef message) {
    encodingAware(message, [=](auto message) { if constexpr (!isUTF16(message)) ThrowIllegalArgumentException(); });
}

extern "C" const KChar* Kotlin_String_utf16pointer(KConstRef message) {
    RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
    Kotlin_String_ensureUTF16(message);
    return reinterpret_cast<const KChar*>(StringRawData(message));
}

extern "C" KInt Kotlin_String_utf16length(KConstRef message) {
    RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
    Kotlin_String_ensureUTF16(message);
    return StringRawSize(message, false);
}

extern "C" KConstNativePtr Kotlin_Arrays_getStringAddressOfElement(KConstRef thiz, KInt index) {
    return encodingAware(thiz, [=](auto thiz) { return reinterpret_cast<KConstNativePtr>(boundsCheckedIteratorAt(thiz, index).ptr()); });
}

std::string kotlin::to_string(KConstRef kstring, KStringConversionMode mode, size_t start, size_t size) {
    RuntimeAssert(kstring->type_info() == theStringTypeInfo, "A Kotlin String expected");
    return encodingAware(kstring, [=](auto kstring) { return kstring.toUTF8(mode, start, size); });
}
