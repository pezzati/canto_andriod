//
// Created by hamed on 12/30/18.
//
#include <jni.h>
#include <string>

static const char *CAFEBAZAAR_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCxRYGrEGg1KPkWrISv6voZqsic/vJDENa/girT8IWn1oUqZIx3a7xJlsRbEtvH/DIW23wpPX4St8A9haTTvZXs0fSLTL8S7jl+3kCyfi9JdHOrlnY6OvooUFJPz4ffNXSSbdKJsbnavFELYeX7M/5H/GQWkwND+yLTkkkONmAKY9V8952TwksF/YwyDA2/0xcbNi6Nk4RbD/oNDLf//c0kC6WY66lDoyg0OPCFDCMCAwEAAQ==";
static const char *IRANAPPS_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChvOsx8dSrMwugdxCwa71QKZMorY+/sxyJe/virUQQqRpXOZfCVbMP69JhV+hhK7pBAw67EXVSSJ7hejrmR+3OopuaKNLvARiTfQ0gX7idtZndNrlwxSAYrHOYCScSc2iHdzaaHpq3fwmYOqxG+/hKK6YskafEvxDWNeZl67XLYQIDAQAB";
static const char *MYKET_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDOvajX1w2+aEFLyHFGCFXGRijK+tl8B3rLZQi9y8bKUkWmocmo4jOhrKaqZJu2srdB4/+yvMO9ok8m0MN5LNxjHNz0Wpehigh6MUeq+d+pN6xVEdIJ2hDVb1EMEIBdTlPeGW8qpOLO20Tvv+NZiO8BExUZ27R0+/X2s0YPPdqyPQIDAQAB";

extern "C" JNIEXPORT jstring
Java_com_hmomeni_canto_activities_PaymentActivity_getPaymentKey(
        JNIEnv *__unused env,
        jobject  __unused obj,
        jstring market
) {
    const char *m = env->GetStringUTFChars(market, 0);
    if (strcmp(m, "cafebazaar") == 0) {
        return env->NewStringUTF(CAFEBAZAAR_KEY);
    }

    if (strcmp(m, "iranapps") == 0) {
        return env->NewStringUTF(IRANAPPS_KEY);
    }

    if (strcmp(m, "myket") == 0) {
        return env->NewStringUTF(IRANAPPS_KEY);
    }

    return env->NewStringUTF("");
}