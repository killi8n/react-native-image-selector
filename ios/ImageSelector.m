#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(ImageSelector, RCTEventEmitter)

RCT_EXTERN_METHOD(launchPicker: (RCTResponseSenderBlock)callback)

RCT_EXTERN_METHOD(getPhotos: (int)limit resolver: (RCTPromiseResolveBlock)resolve rejecter: (RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(initializePhotos: (RCTPromiseResolveBlock)resolve rejecter: (RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(supportedEvents)

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
