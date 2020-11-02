#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(ImageSelector, NSObject)

RCT_EXTERN_METHOD(getPhotos: (int)limit resolver: (RCTPromiseResolveBlock)resolve rejecter: (RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(initializePhotos: (RCTPromiseResolveBlock)resolve rejecter: (RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
