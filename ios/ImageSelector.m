#import <React/RCTBridgeModule.h>
//#import <React/RCTEventEmitter.h>

//@interface RCT_EXTERN_MODULE(ImageSelector, RCTEventEmitter)
@interface RCT_EXTERN_MODULE(ImageSelector, NSObject)

RCT_EXTERN_METHOD(launchPicker: (NSDictionary)options responseCallback: (RCTResponseSenderBlock)callback)

//RCT_EXTERN_METHOD(supportedEvents)

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
