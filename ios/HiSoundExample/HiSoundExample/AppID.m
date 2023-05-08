//
//  AppID.m
//  HiSoundExample
//
//  Created by kuozhi.li on 2023/4/3.
//

#import <Foundation/Foundation.h>

NSString *const appID = <#YOUR_APP_ID#>;
/* assign Token to nil if you have not enabled app certificate
 * before you deploy your own token server, you can easily generate a temp token
 * for dev use at https://dashboard.agora.io note the token generated are
 * allowed to join corresponding room ONLY.
 */
/* 如果没有打开鉴权Token, 这里的token值给nil就好
 * 生成Token需要参照官方文档部署Token服务器，开发阶段若想先不部署服务器,
 * 可以在https://dashbaord.agora.io生成 临时Token.
 * 请注意生成Token时指定的频道名, 该Token只允许加入对应的频道
 */
NSString *const certificate = nil;

NSString *const appkey = <#YOUR_APP_KEY#>;

NSString *const secret = <#YOUR_APP_SECRET#>;
