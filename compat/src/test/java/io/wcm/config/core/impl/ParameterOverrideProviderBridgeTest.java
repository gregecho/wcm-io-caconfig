/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2016 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.config.core.impl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;

import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableMap;

import io.wcm.config.spi.ParameterOverrideProvider;
import io.wcm.config.spi.ParameterProvider;
import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("null")
public class ParameterOverrideProviderBridgeTest {

  private static final Map<String, String> OVERRIDES_LEGACY = ImmutableMap.<String, String>builder()
      .put("[/content/a/b]stringParam", "value1")
      .put("stringArrayParam", "value2")
      .put("intParam", "555")
      .put("[/content:locked]longParam", "666")
      .put("doubleParam", "1.23")
      .put("[default]boolParam", "true")
      .put("otherParam", "value3")
      .build();

  @Rule
  public AemContext context = new AemContext();

  @Mock
  private ConfigurationManager configManager;

  private ParameterOverrideProviderBridge underTest;
  private ConfigurationMetadataProvider configMetadataProvider;

  @Before
  public void setUp() {
    context.registerService(ConfigurationManager.class, configManager);
    configMetadataProvider = context.registerInjectActivateService(new ParameterProviderBridge());
    when(configManager.getConfigurationMetadata(anyString())).thenAnswer(new Answer<ConfigurationMetadata>() {
      @Override
      public ConfigurationMetadata answer(InvocationOnMock invocation) throws Throwable {
        return configMetadataProvider.getConfigurationMetadata((String)invocation.getArgument(0));
      }
    });
    underTest = context.registerInjectActivateService(new ParameterOverrideProviderBridge());
  }

  @Test
  public void testWithMetadata() {
    context.registerService(ParameterProvider.class, new DummyParameterProvider());
    context.registerService(ParameterOverrideProvider.class, new ParameterOverrideProvider() {
      @Override
      public Map<String, String> getOverrideMap() {
        return OVERRIDES_LEGACY;
      }
    });

    Collection<String> overrides = underTest.getOverrideStrings();
    assertEquals(7, overrides.size());
    assertThat(overrides, containsInAnyOrder(
        "[/content/a/b]stringParam=\"value1\"",
        "stringArrayParam=[\"value2\"]",
        "intParam=555",
        "[/content]longParam=666",
        "doubleParam=1.23",
        "boolParam=true",
        "otherParam=\"value3\""));
  }


  @Test
  public void testNoMetadata() {
    context.registerService(ParameterOverrideProvider.class, new ParameterOverrideProvider() {
      @Override
      public Map<String, String> getOverrideMap() {
        return OVERRIDES_LEGACY;
      }
    });

    Collection<String> overrides = underTest.getOverrideStrings();
    assertEquals(7, overrides.size());
    assertThat(overrides, containsInAnyOrder(
        "[/content/a/b]stringParam=\"value1\"",
        "stringArrayParam=\"value2\"",
        "intParam=\"555\"",
        "[/content]longParam=\"666\"",
        "doubleParam=\"1.23\"",
        "boolParam=\"true\"",
        "otherParam=\"value3\""));
  }

  @Test
  public void testNoOverrides() {
    assertTrue(underTest.getOverrideStrings().isEmpty());
  }

}
