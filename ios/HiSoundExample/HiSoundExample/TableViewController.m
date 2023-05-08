//
//  TableViewController.m
//  HiSoundExample
//
//  Created by kuozhi.li on 2023/4/3.
//

#import "TableViewController.h"
#import "TableViewCell.h"

@interface TableViewController()
@property(nonatomic, strong) NSArray *effect_names;
//@property(nonatomic, strong) NSArray *effect_paths;
@property(nonatomic, strong) NSDictionary *effect_dict;
//@property(nonatomic, strong) NSString *work_dir;
@end

@implementation TableViewController

- (void) viewDidLoad{
    [super viewDidLoad];
    //    self.effects = [NSArray arrayWithObjects: @"", @"", @"", nil];
//    self.effect_paths = @[
//        @"",
//        [NSBundle.mainBundle pathForResource:@"effect" ofType:@"bin" inDirectory:@"hisound_effect/超重低音"],
//        [NSBundle.mainBundle pathForResource:@"effect" ofType:@"bin" inDirectory:@"hisound_effect/剧院混响"],
//        [NSBundle.mainBundle pathForResource:@"effect" ofType:@"bin" inDirectory:@"hisound_effect/人声独享"],
//    ];
    
    self.effect_dict = @{
        @"无音效": @"",
        @"超重低音":[NSBundle.mainBundle pathForResource:@"effect" ofType:@"bin" inDirectory:@"hisound_effect/超重低音"],
        @"剧院混响":[NSBundle.mainBundle pathForResource:@"effect" ofType:@"bin" inDirectory:@"hisound_effect/剧院混响"],
        @"人声独享":[NSBundle.mainBundle pathForResource:@"effect" ofType:@"bin" inDirectory:@"hisound_effect/人声独享"],
    };
    
    self.effect_names = [self.effect_dict allKeys];
    
//    self.effect_names = @[@"无音效"];
//    for(int i = 1; i < self.effect_paths; i++){
//
//    }
//    NSLog(@"effects: %@", self.effect_names[1]);
//    NSLog(@"effects: %@", self.effect_dict[1]);
}

#pragma mask - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
    return self.effect_dict.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath{
    TableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"cellreusedID" forIndexPath:indexPath];
    NSString *effect_name = [self.effect_names objectAtIndex:indexPath.row];
   
//    NSString *effect = [self.effect_dict objectForKey:effect_name];
    cell.effect.text = effect_name;
    return cell;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(nonnull NSIndexPath *)indexPath{
    return NO;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(nonnull NSIndexPath *)indexPath{
    NSString *effect_name = [self.effect_names objectAtIndex:indexPath.row];
    NSString *effect_path = [self.effect_dict objectForKey:effect_name];
    NSLog(@"effect name: %@ effect path: %@", effect_name, effect_path);
    self.effectBlock(effect_name, effect_path);
    [self dismissViewControllerAnimated:true completion:nil];
}

@end

