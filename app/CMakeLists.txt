cmake_minimum_required(VERSION 3.4.1)

# convert SDK path to forward slashes on Windows
file(TO_CMAKE_PATH ${PATH_TO_SUPERPOWERED} PATH_TO_SUPERPOWERED)

set(CMAKE_VERBOSE_MAKEFILE on)

include_directories(src/main/cpp)
include_directories(${PATH_TO_SUPERPOWERED})

# compile player example to a native library
#[[add_library(
        native-lib
        SHARED
        src/main/cpp/native-lib.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)]]

#add_library(
#        Player
#        SHARED
#        src/main/cpp/Player.cpp
#        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
#)
add_library(
        Dubsmash
        SHARED
        src/main/cpp/Dubsmash.cpp
        ${PATH_TO_SUPERPOWERED}/AndroidIO/SuperpoweredAndroidAudioIO.cpp
)
# link the native library against the following libraries
target_link_libraries(
        #        native-lib
        Dubsmash
        #        Player
        ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
        OpenSLES
        log
        android
)