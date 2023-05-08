Pod::Spec.new do |s|
    s.name             = 'HiSoundExample'
    s.version          = '0.1.0'
    s.summary          = 'ByteDance effect plugin for Agora RTE extensions.'
    s.description      = 'project.description'
    s.homepage         = 'https://github.com/AgoraIO-Community/AgoraMarketPlace'
    s.author           = { 'Agora' => 'developer@agora.io' }
    s.source           = { :path => '.' }
    # s.vendored_frameworks = 'agora-hisound.xcframework', 'HiSound.xcframework'
    s.vendored_frameworks = 'agora-hisound.xcframework', 'HiSound.xcframework', 'agora-hisound-deps.xcframework'
    s.platform = :ios, '9.0'
  end
  
