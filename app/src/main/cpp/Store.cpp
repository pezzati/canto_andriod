//
// Created by hamed on 12/30/18.
//
#include <jni.h>
#include <string>

static const char *CAFEBAZAAR_KEY = "";

extern "C" JNIEXPORT jstring
Java_com_hmomeni_canto_activities_PaymentActivity_getPaymentKey(
        JNIEnv *__unused env,
        jobject  __unused obj,
        jstring market
) {
    const char *m = env->GetStringUTFChars(market, 0);
    if (m == "cafebazaar") {
        return env->NewStringUTF(CAFEBAZAAR_KEY);
    }

    return env->NewStringUTF("");
}