cmake_minimum_required(VERSION 3.22.1)
project(Hunspell)

add_library(
        hunspell
        SHARED
        src/main/cpp/hunspell/affentry.cxx
        src/main/cpp/hunspell/affixmgr.cxx
        src/main/cpp/hunspell/csutil.cxx
        src/main/cpp/hunspell/filemgr.cxx
        src/main/cpp/hunspell/hashmgr.cxx
        src/main/cpp/hunspell/hunspell.cxx
        src/main/cpp/hunspell/hunzip.cxx
        src/main/cpp/hunspell/phonet.cxx
        src/main/cpp/hunspell/replist.cxx
        src/main/cpp/hunspell/suggestmgr.cxx
        src/main/cpp/hunspell_jni.cpp
)

target_include_directories(hunspell PUBLIC src/main/cpp/hunspell)