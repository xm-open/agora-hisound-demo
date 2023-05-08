//
//  TableViewController.h
//  HiSoundExample
//
//  Created by kuozhi.li on 2023/4/3.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^EffectSelectBlock)(NSString *effect_name, NSString *effect_path);
@interface TableViewController: UITableViewController
@property(nonatomic, strong) EffectSelectBlock effectBlock;
@end

NS_ASSUME_NONNULL_END
