/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfiguredComponent;
import org.mule.runtime.extension.api.runtime.source.ParameterizedSource;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;

public class MessageSourceUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageSourceUtils.class);

  /**
   * Extracts the configured HTTP URI from a flow. It only works for flows that uses the HTTP extension.
   *
   */
  public static URI getUriFromFlow(Component source) {
    if (source != null && isHttpExtensionSource(source)) {
      try {
        String resolvedPath = getListenerPath(source);
        return buildListenerUri(getConfigState(source).getConnectionParameters(), resolvedPath);
      } catch (Exception e) {
        LOGGER.warn("Error getting uri from flow " + source.getLocation().getRootContainerName(), e);
      }
    }

    return null;
  }

  private static String getListenerPath(Component source) {
    ParameterizedSource parameterizedSource = ((ParameterizedSource) source);
    String listenerPath = ((String) parameterizedSource.getInitialisationParameters().get("path"));
    String basePath = ((String) getConfigState(source).getConfigParameters().get("basePath"));
    listenerPath = prependIfMissing(listenerPath, "/");
    return basePath == null ? listenerPath : prependIfMissing(basePath, "/") + listenerPath;
  }

  private static ConfigurationState getConfigState(Component source) {
    ConfiguredComponent configuredComponent = ((ConfiguredComponent) source);
    return configuredComponent.getConfigurationInstance()
        .orElseThrow(() -> new RuntimeException("Source does not contain a configuration instance"))
        .getState();
  }

  private static boolean isHttpExtensionSource(Component source) {
    final ComponentIdentifier identifier = source.getLocation().getComponentIdentifier().getIdentifier();
    return identifier.equals(buildFromStringRepresentation("http:listener"));
  }

  private static URI buildListenerUri(Map<String, Object> connectionParams, String path) throws URISyntaxException {
    String host = ((String) connectionParams.get("host"));
    Integer port = ((Integer) connectionParams.get("port"));
    String scheme = connectionParams.get("protocol").toString().toLowerCase();
    return new URI(scheme, null, host, port, path, null, null);
  }

}
