//
// Created by hamed on 10/31/18.
//
#include <jni.h>
#include <android/log.h>
#include <string>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredSimple.h>

#define log_write __android_log_write
#define log_print __android_log_print

const char *TAG = "DubsmashCPP";

static SuperpoweredAndroidAudioIO *audioIO;