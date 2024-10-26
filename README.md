## OneUI6 Design Lib and sample app

This design lib is intended to complement and integrate with both [SESL6 Android Jetpack Modules](https://github.com/tribalfs/sesl-androidx?tab=readme-ov-file#sesloneui-android-jetpack-unofficial) 
and [SESL6 Material Components for Android](https://github.com/tribalfs/sesl-material-components-android?tab=readme-ov-file#sesloneui-material-components-for-android-unofficial).

## Usage
Add the needed [SESL6 Android Jetpack Modules](https://github.com/tribalfs/sesl-androidx?tab=readme-ov-file#sesloneui-android-jetpack-unofficial)
and [SESL6 Material Components for Android](https://github.com/tribalfs/sesl-material-components-android?tab=readme-ov-file#sesloneui-material-components-for-android-unofficial) 
dependencies to your project following their usage guide. Then add the following dependency next:

```
repositories {
  //other remote repositories
  
   maven {
      url = uri("https://maven.pkg.github.com/tribalfs/oneui-design")
      credentials {
          username = "<gh_username>"
          password = "<gh_access_token>"
      }
   } 
}
```

```
dependencies {
  //sesl and other dependencies
  
  implementation("io.github.tribalfs:oneui-design:0.1.1+oneui6")
}
```


<a href="https://github.com/tribalfs/oneui-design-sampleapp/raw/sample_setup_sesl6/sample-app/release/sample-app-release.apk">Download Sample APK</a>

### Credits
- [Google](https://developer.android.com/jetpack) for their Jetpack and Material Components libraries.
- [Samsung](https://www.samsung.com/) for their awesome OneUI Design.
- [Yanndroid](https://github.com/Yanndroid) and [Salvo Giangreco](https://github.com/salvogiangri) who created the [OneUI4 Design library](https://github.com/OneUIProject/oneui-design) where this repository came from. 
