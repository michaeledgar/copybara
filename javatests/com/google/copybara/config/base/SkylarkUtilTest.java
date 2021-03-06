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

package com.google.copybara.config.base;

import static com.google.common.truth.Truth.assertThat;

import com.google.copybara.config.SkylarkUtil;
import com.google.devtools.build.lib.events.Location;
import com.google.devtools.build.lib.syntax.EvalException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SkylarkUtilTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testCheckNotEmpty_Null() throws Exception {
    thrown.expect(EvalException.class);
    thrown.expectMessage("Invalid empty field 'foo'");
    SkylarkUtil.checkNotEmpty(null, "foo", Location.BUILTIN);
  }

  @Test
  public void testCheckNotEmpty_Empty() throws Exception {
    thrown.expect(EvalException.class);
    thrown.expectMessage("Invalid empty field 'foo'");
    SkylarkUtil.checkNotEmpty("", "foo", Location.BUILTIN);
  }

  @Test
  public void testCheckNotEmpty_NonEmpty() throws Exception {
    assertThat(SkylarkUtil.checkNotEmpty("test", "foo", Location.BUILTIN)).isEqualTo("test");
  }
}
