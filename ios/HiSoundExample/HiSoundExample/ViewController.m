//
//  ViewController.m
//  HiSoundExample
//
//  Created by kuozhi.li on 2023/4/3.
//

#import "ViewController.h"
#import "AppID.h"
#import "TableViewController.h"
#import <AgoraRtcKit/AgoraRtcEngineKit.h>

@interface ViewController ()<AgoraRtcEngineDelegate,
AgoraMediaFilterEventDelegate,
UIPopoverPresentationControllerDelegate>
@property(strong, nonatomic) AgoraRtcEngineKit *agoraKit;
@property(assign, nonatomic) BOOL enable;
@property(weak, nonatomic) IBOutlet UITextField *channelField;
@property(strong, nonatomic) NSString *channel;
@property(assign, nonatomic) NSUInteger uid;
@property(strong, nonatomic) NSString *json_init;
@property(strong, nonatomic) NSString *dir_init;
@property(assign, nonatomic) BOOL has_join;
@property(weak, nonatomic) IBOutlet UIButton *joinBtn;
@property(weak, nonatomic) IBOutlet UIButton *startBtn; // init hisound
@property(weak, nonatomic) IBOutlet UIButton *switchBtn; // 切换音效
@property(weak, nonatomic) IBOutlet UITextField *currentEffect;

@end

@implementation ViewController

//NSString *EXTENSION_NAME = @"agora-hisound-filter";
NSString *EXTENSION_VENDOR_NAME = @"Ximalaya";
NSString *EXTENSION_AUDIO_FILTER = @"HiSound";

NSString *KEY_INIT_HISOUND = @"init_hisound";
NSString *KEY_STOP_HISOUND = @"stop_effect";
NSString *KEY_LOAD_CONFIG = @"load_effect";

NSString *EFFECT_WORK_DIR = @"work_dir";

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    //1.获取textField所在的父视图self.view
    [self.view endEditing:YES];
    //2.直接调用  注销第一响应者
    //[self.textField resignFirstResponder];
}

- (void)viewDidLoad {
    self.json_init = [NSBundle.mainBundle pathForResource:@"init" ofType:@"json" inDirectory:@"hisound_effect"];
    self.dir_init = [self.json_init stringByDeletingLastPathComponent];
    if (![self.dir_init hasSuffix:@"/"]) {
        self.dir_init = [self.dir_init stringByAppendingString:@"/"];
    }
    self.has_join = false;
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [self initRtcEngine];
}

- (void)initRtcEngine {
    AgoraRtcEngineConfig *config = [AgoraRtcEngineConfig new];
    config.appId = appID;
    // 监听插件事件，用于接收 onEvent 回调
    config.eventDelegate = self;
    self.agoraKit = [AgoraRtcEngineKit sharedEngineWithConfig:config
                                                     delegate:self];
    [self enableExtension:nil];
    int ret = [self.agoraKit enableAudio];
    NSLog(@"enableAudio ret: %d", ret);
    ret = [self.agoraKit setChannelProfile:AgoraChannelProfileLiveBroadcasting];
    NSLog(@"setChannelProfile: %d", ret);
    ret = [self.agoraKit setClientRole:AgoraClientRoleBroadcaster];
    NSLog(@"setClientRole: %d", ret);
}

- (void) enableExtension:(id)sender{
    self.enable = !self.enable;
    int ret = [self.agoraKit enableExtensionWithVendor:EXTENSION_VENDOR_NAME
                                             extension:EXTENSION_AUDIO_FILTER
                                               enabled:self.enable];
    NSLog(@"enableExtension, ret: %d", ret);
    //    if (self.enable) {
    //      [self.enableExtensionBtn setTitle:@"disableExtension"
    //                               forState:UIControlStateNormal];
    //    } else {
    //      [self.enableExtensionBtn setTitle:@"enableExtension"
    //                               forState:UIControlStateNormal];
    //    }
}

- (IBAction)startHiSound:(id)sender{
    NSError *error;
    NSData *data = [NSJSONSerialization
                    dataWithJSONObject:@{
                        @"appkey": appkey,
                        @"secret": secret,
                        @"init_json": self.json_init,
                        @"init_dir": self.dir_init
                    }
                    options:NSJSONWritingPrettyPrinted
                    error:&error];
    int ret = [self.agoraKit
               setExtensionPropertyWithVendor:EXTENSION_VENDOR_NAME
               extension:EXTENSION_AUDIO_FILTER
               key:KEY_INIT_HISOUND
               value:[[NSString alloc]
                      initWithData:data
                      encoding:NSUTF8StringEncoding]];
    NSLog(@"setExtensionPropertyWithVendor, ret: %d", ret);
    //    if(ret != 0){
    //        NSLog(@"init hisound failed");
    //    } else {
    [self.startBtn setEnabled:false];
    [self.switchBtn setEnabled:true];
    //    }
}

typedef void (^leaveChannelBlock)(AgoraChannelStats * _Nonnull stat);

- (IBAction)joinChannel:(id)sender{
    self.channel = self.channelField.text;
    NSLog(@"channel: %@, hash_join: %d", self.channel, self.has_join);
    if([self.channel length] == 0){
        NSLog(@"please input channel name");
        UIAlertController* alert = [UIAlertController alertControllerWithTitle:@" Alert"
                                                                       message:@"请输入频道名"
                                                                preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault
                                                              handler:^(UIAlertAction * action) {}];
        
        [alert addAction:defaultAction];
        [self presentViewController:alert animated:YES completion:nil];
        return ;
    }
    
    if(self.has_join){
        int ret = [self.agoraKit leaveChannel:nil]; // callbcak
        NSLog(@"leaveChannel ret: %d", ret);
        [self.joinBtn setTitle:@"加入频道" forState:UIControlStateNormal];
        [self.startBtn setEnabled:false];
        [self.switchBtn setEnabled:false];
        [self.currentEffect setText:@"当前音效：无音效"];
        self.has_join = false;
    } else {
        NSString *accessToken = certificate;
        int ret = [self.agoraKit joinChannelByToken:accessToken
                                      channelId:self.channel
                                           info:nil
                                            uid:0
                                    joinSuccess:^(NSString * _Nonnull channel, NSUInteger uid, NSInteger elapsed) {
            NSLog(@"joinseSuccess");
            self.uid = uid;
        }];
        NSLog(@"joinChannelByToken: %d", ret);
        [self.joinBtn setTitle:@"离开频道" forState:UIControlStateNormal];
        [self.startBtn setEnabled:true];
        self.has_join = true;
    }
}

- (void)onEvent:(NSString *__nullable)provider
      extension:(NSString *__nullable)extension
            key:(NSString *__nullable)key
          value:(NSString *__nullable)value {
    NSLog(@"provider: %@, extension: %@, key: %@, value: %@", provider, extension,
          key, value);
}

- (void)onExtensionStopped:(NSString * __nullable)provider
                 extension:(NSString * __nullable)extension NS_SWIFT_NAME(onExtensionStopped(_:extension:)){
    NSLog(@"onExtensionStopped, provider: %@, extension: %@", provider, extension);
}

- (void)onExtensionStarted:(NSString * __nullable)provider
                 extension:(NSString * __nullable)extension NS_SWIFT_NAME(onExtensionStarted(_:extension:)){
    NSLog(@"onExtensionStarted, provider: %@, extension: %@", provider, extension);
}

- (void)onExtensionError:(NSString * __nullable)provider
               extension:(NSString * __nullable)extension
                   error:(int)error
                 message:(NSString * __nullable)message{
    NSLog(@"onExtensionError, error: %d, message: %@", error, message);
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender{
    if ([segue.identifier isEqualToString:@"PopSegue"]){
        TableViewController *controller = segue.destinationViewController;
        // 设置Controller尺寸
        controller.preferredContentSize = CGSizeMake(200, 400);
        
        UIPopoverPresentationController *popController =
        controller.popoverPresentationController;
        __weak typeof(self) weakSelf = self;
        controller.effectBlock = ^(NSString *_Nonnull effect_name, NSString *_Nonnull effect_path){
            if(!weakSelf) { return ;}
//            NSLog(@"effect_name: %@, effect_path: %@", effect_name, effect_path);
            NSError *error;
            if([effect_name isEqualToString:@"无音效"]){
                NSData *data = [NSJSONSerialization
                                dataWithJSONObject:@{}
                                options:NSJSONWritingPrettyPrinted
                                error:&error];
                [self.agoraKit
                 setExtensionPropertyWithVendor:EXTENSION_VENDOR_NAME
                 extension:EXTENSION_AUDIO_FILTER
                 key:KEY_STOP_HISOUND
                 value:[[NSString alloc]
                        initWithData:data
                        encoding:NSUTF8StringEncoding]];
                NSString *new_text = [NSString stringWithFormat:@"当前音效：%@", effect_name];
                [self.currentEffect setText: new_text];
                // 设置箭头的“尖儿”所指向的位置
                popController.sourceRect = CGRectMake(0, 50, 100, 0);
                if (popController) { popController.delegate = self; }
                return ;
            }
            NSString *work_dir = [effect_path stringByDeletingLastPathComponent];
            NSData *data = [NSJSONSerialization
                            dataWithJSONObject:@{
                EFFECT_WORK_DIR: work_dir
            }
                            options:NSJSONWritingPrettyPrinted
                            error:&error];
            [self.agoraKit
             setExtensionPropertyWithVendor:EXTENSION_VENDOR_NAME
             extension:EXTENSION_AUDIO_FILTER
             key:KEY_LOAD_CONFIG
             value:[[NSString alloc]
                    initWithData:data
                    encoding:NSUTF8StringEncoding]];
            NSString *new_text = [NSString stringWithFormat:@"当前音效：%@", effect_name];
            [self.currentEffect setText:new_text];
            // 设置箭头的“尖儿”所指向的位置
            popController.sourceRect = CGRectMake(0, 50, 100, 0);
            if (popController) { popController.delegate = self; }
        };
    }
}

// UIPopoverPresentationControllerDelegate中的代理方法，当前controller需要遵守此协议
- (UIModalPresentationStyle)adaptivePresentationStyleForPresentationController:
(UIPresentationController *)controller {
    // 设置弹出的样式
    return UIModalPresentationNone;
}


@end
