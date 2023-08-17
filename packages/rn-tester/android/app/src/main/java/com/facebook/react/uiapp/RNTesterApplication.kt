/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.uiapp;

import android.app.Application;
import androidx.annotation.NonNull;
import com.facebook.fbreact.specs.SampleTurboModule;
import com.facebook.react.JSEngineResolutionAlgorithm;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridgeless.ReactHostImpl;
import com.facebook.react.common.annotations.UnstableReactNativeAPI;
import com.facebook.react.common.assets.ReactFontManager;
import com.facebook.react.common.mapbuffer.ReadableMapBuffer;
import com.facebook.react.config.ReactFeatureFlags;
import com.facebook.react.defaults.DefaultComponentsRegistry;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactNativeHost;
import com.facebook.react.fabric.ComponentFactory;
import com.facebook.react.flipper.ReactNativeFlipper;
import com.facebook.react.interfaces.ReactHost;
import com.facebook.react.interfaces.exceptionmanager.ReactJsExceptionHandler;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.uiapp.component.MyLegacyViewManager;
import com.facebook.react.uiapp.component.MyNativeViewManager;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.soloader.SoLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RNTesterApplication extends Application implements ReactApplication {
  
  private lateinit var mReactHost: ReactHostImpl

  private val mReactNativeHost = object: DefaultReactNativeHost(this) {
    override fun getJSMainModuleName(): String {
      return "js/RNTesterApp.android"
    }

    override fun getBundleAssetName(): String {
      return "RNTesterApp.android.bundle"
    }

    override fun getUseDeveloperSupport(): Boolean {
      return BuildConfig.DEBUG
    }

    override fun getPackages(): List<ReactPackage> {
      return listOf(
        MainReactPackage(),
        TurboReactPackage() {
          fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule {
            if (!ReactFeatureFlags.useTurboModules) {
              return null;
            }

            if (SampleTurboModule.NAME.equals(name)) {
              return SampleTurboModule(reactContext)
            }

            return null
          }

        // Note: Specialized annotation processor for @ReactModule isn't configured in OSS
        // yet. For now, hardcode this information, though it's not necessary for most
        // modules.
        fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
          return object: ReactModuleInfoProvider() {
            override fun getReactModuleInfos(): MutableMap<String, ReactModuleInfo> {
              val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
              if (ReactFeatureFlags.useTurboModules) {
                moduleInfos[SampleTurboModule.NAME] = ReactModuleInfo(
                  SampleTurboModule.NAME,
                  "SampleTurboModule",
                  false, // canOverrideExistingModule
                  false, // needsEagerInit
                  true, // hasConstants
                  false, // isCxxModule
                  true // isTurboModule
                )
              }
              return moduleInfos
            }
          }
        }
      },
      ReactPackage() {
        @NonNull
        override fun createNativeModules(@NonNull reactContext: ReactApplicationContext): List<NativeModule> {
          return emptyList()
        }

        @NonNull
        override fun createViewManagers(@NonNull reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
          val viewManagers: MutableList<ViewManager<*, *>> = mutableListOf()
          viewManagers.add(MyNativeViewManager())
          viewManagers.add(MyLegacyViewManager(reactContext))
          return viewManagers
        }
      })
    }

    override fun isNewArchEnabled(): Boolean {
      return BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
    }

    override fun isHermesEnabled(): Boolean {
      return BuildConfig.IS_HERMES_ENABLED_IN_FLAVOR
    }
  }

  override fun onCreate(){
    ReactFontManager.getInstance().addCustomFont(this, "Rubik", R.font.rubik)
    super.onCreate()
    SoLoader.init(this, /* native exopackage */ false)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
        DefaultNewArchitectureEntryPoint.load()
    }
    if (ReactFeatureFlags.enableBridgelessArchitecture) {
        // TODO: initialize Flipper for Bridgeless
    } else {
        ReactNativeFlipper.initializeFlipper(this, getReactNativeHost().getReactInstanceManager())
    }
  }

  override fun getReactNativeHost(): ReactNativeHost {
    if (ReactFeatureFlags.enableBridgelessArchitecture) {
      throw RuntimeException("Should not use ReactNativeHost when Bridgeless enabled");
    }
    return mReactNativeHost
  }

  @UnstableReactNativeAPI
  override fun getReactHostInterface(): ReactHost {
    if (mReactHost == null) {
      // Create an instance of ReactHost to manager the instance of ReactInstance,
      // which is similar to how we use ReactNativeHost to manager instance of ReactInstanceManager
      val reactHostDelegate: RNTesterReactHostDelegate = RNTesterReactHostDelegate(getApplicationContext())
      val reactJsExceptionHandler = RNTesterReactJsExceptionHandler()

      val componentFactory: ComponentFactory = ComponentFactory()
      DefaultComponentsRegistry.register(componentFactory)
      mReactHost = ReactHostImpl(
          this.getApplicationContext(),
          reactHostDelegate,
          componentFactory,
          true,
          reactJsExceptionHandler,
          true);
      if (BuildConfig.IS_HERMES_ENABLED_IN_FLAVOR) {
        mReactHost.setJSEngineResolutionAlgorithm(JSEngineResolutionAlgorithm.HERMES);
      } else {
        mReactHost.setJSEngineResolutionAlgorithm(JSEngineResolutionAlgorithm.JSC);
      }
      reactHostDelegate.setReactHost(mReactHost);
    }
    return mReactHost
  }

  @UnstableReactNativeAPI
  class RNTesterReactJsExceptionHandler : ReactJsExceptionHandler {
    override fun reportJsException(errorMap: ReadableMapBuffer) {}
  } 
}
