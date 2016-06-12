// Copyright 2015 The Bazel Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.testing.junit.runner.junit4;

import static dagger.Provides.Type.SET;

import com.google.common.base.Optional;
import com.google.common.base.Ticker;
import com.google.common.io.ByteStreams;
import com.google.testing.junit.runner.internal.SignalHandlers;
import com.google.testing.junit.runner.util.TestNameProvider;

import dagger.Module;
import dagger.Provides;

import org.junit.runner.notification.RunListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.inject.Singleton;

/**
 * Dagger module for real test runs.
 */
@Module(includes = {JUnit4RunnerBaseModule.class, JUnit4InstanceModules.Config.class})
public final class JUnit4RunnerModule {
  @Provides
  static Ticker ticker() {
    return Ticker.systemTicker();
  }

  @Provides
  static SignalHandlers.HandlerInstaller signalHandlerInstaller() {
    return SignalHandlers.createRealHandlerInstaller();
  }

  @Provides(type = SET)
  static RunListener nameListener(JUnit4TestNameListener impl) {
    return impl;
  }

  @Provides(type = SET)
  static RunListener xmlListener(JUnit4TestXmlListener impl) {
    return impl;
  }

  @Provides(type = SET)
  static RunListener stackTraceListener(JUnit4TestStackTraceListener impl) {
    return impl;
  }


  @Provides
  @Singleton
  @Xml
  static OutputStream provideXmlStream(JUnit4Config config) {
    Optional<Path> path = config.getXmlOutputPath();

    if (path.isPresent()) {
      try {
        // TODO(bazel-team): Change the provider method to return ByteSink or CharSink
        return new FileOutputStream(path.get().toFile());
      } catch (FileNotFoundException e) {
        /*
         * We try to avoid throwing exceptions in the runner code. There is no
         * way to induce a test failure here, so the only thing we can do is
         * print a message and move on.
         */
        e.printStackTrace();
      }
    }

    return ByteStreams.nullOutputStream();
  }

  @Provides @Singleton
  SettableCurrentRunningTest provideCurrentRunningTest() {
    return new SettableCurrentRunningTest() {
      @Override
      void setGlobalTestNameProvider(TestNameProvider provider) {
        testNameProvider = provider;
      }
    };
  }
}
