/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.testing;

import com.google.copybara.GeneralOptions;
import com.google.copybara.Option;
import com.google.copybara.Options;
import com.google.copybara.config.OptionsAwareModule;
import com.google.devtools.build.lib.skylarkinterface.Param;
import com.google.devtools.build.lib.skylarkinterface.SkylarkModule;
import com.google.devtools.build.lib.skylarkinterface.SkylarkModuleCategory;
import com.google.devtools.build.lib.skylarkinterface.SkylarkSignature;
import com.google.devtools.build.lib.syntax.BuiltinFunction;
import com.google.devtools.build.lib.syntax.EvalException;

/**
 * A Skylark module used by tests
 */
@SkylarkModule(
    name = "testing",
    doc = "",
    category = SkylarkModuleCategory.BUILTIN)
public class TestingModule implements OptionsAwareModule {

  private TestingOptions testingOptions;

  @Override
  public void setOptions(Options options) {
    testingOptions = options.get(TestingOptions.class);
    options.get(GeneralOptions.class);
  }

  @SkylarkSignature(name = "origin", returnType = DummyOrigin.class,
      doc = "A dummy origin",
      parameters = {
          @Param(name = "self", type = TestingModule.class, doc = "this object"),
      },
      objectType = TestingModule.class)
  public static final BuiltinFunction ORIGIN = new BuiltinFunction("origin") {
    public DummyOrigin invoke(TestingModule self) throws EvalException {
      return self.testingOptions.origin;
    }
  };

  @SkylarkSignature(name = "destination",
      returnType = RecordsProcessCallDestination.class,
      doc = "A dummy destination",
      parameters = {
          @Param(name = "self", type = TestingModule.class, doc = "this object"),
      },
      objectType = TestingModule.class)
  public static final BuiltinFunction DESTINATION = new BuiltinFunction("destination") {
    public RecordsProcessCallDestination invoke(TestingModule self) throws EvalException {
      return self.testingOptions.destination;
    }
  };

  @SkylarkSignature(
    name = "dummy_endpoint",
    returnType = DummyEndpoint.class,
    doc = "A dummy feedback endpoint",
    parameters = {
      @Param(name = "self", type = TestingModule.class, doc = "this object"),
    },
    objectType = TestingModule.class
  )
  public static final BuiltinFunction FEEDBACK_ENDPOINT =
      new BuiltinFunction("dummy_endpoint") {
        public DummyEndpoint invoke(TestingModule self) throws EvalException {
          return self.testingOptions.feedbackTrigger;
        }
      };

  @SkylarkSignature(
      name = "dummy_trigger",
      returnType = DummyTrigger.class,
      doc = "A dummy feedback trigger",
      parameters = {
        @Param(name = "self", type = TestingModule.class, doc = "this object"),
      },
      objectType = TestingModule.class)
  public static final BuiltinFunction FEEDBACK_TRIGGER =
      new BuiltinFunction("dummy_trigger") {
        public DummyTrigger invoke(TestingModule self) throws EvalException {
          return self.testingOptions.feedbackTrigger;
        }
      };

  @SkylarkSignature(
      name = "dummy_checker",
      returnType = DummyChecker.class,
      doc = "A dummy checker",
      parameters = {
        @Param(name = "self", type = TestingModule.class, doc = "this object"),
      },
      objectType = TestingModule.class)
  public static final BuiltinFunction CHECKER =
      new BuiltinFunction("dummy_checker") {
        public DummyChecker invoke(TestingModule self) throws EvalException {
          return self.testingOptions.checker;
        }
      };

  public final static class TestingOptions implements Option {

    public DummyOrigin origin;
    public RecordsProcessCallDestination destination;

    public DummyTrigger feedbackTrigger;
    public DummyChecker checker;
  }
}
