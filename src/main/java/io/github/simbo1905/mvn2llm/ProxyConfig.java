package io.github.simbo1905.mvn2llm;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Optional;

record ProxyConfig(
    Optional<URI> httpProxy,
    Optional<URI> httpsProxy
) {
  static ProxyConfig create(MainArguments args) {
    // Command line arguments take precedence
    var httpProxy = Optional.ofNullable(args.httpProxy())
        .map(URI::create);
    var httpsProxy = Optional.ofNullable(args.httpsProxy())
        .map(URI::create);

    // Fall back to environment variables if not specified in args
    if (httpProxy.isEmpty()) {
      httpProxy = Optional.ofNullable(System.getenv("HTTP_PROXY"))
          .or(() -> Optional.ofNullable(System.getenv("http_proxy")))
          .map(URI::create);
    }

    if (httpsProxy.isEmpty()) {
      httpsProxy = Optional.ofNullable(System.getenv("HTTPS_PROXY"))
          .or(() -> Optional.ofNullable(System.getenv("https_proxy")))
          .map(URI::create);
    }

    return new ProxyConfig(httpProxy, httpsProxy);
  }

  ProxySelector toProxySelector() {
    return ProxySelector.of(new InetSocketAddress(
        httpsProxy.orElse(httpProxy.orElseThrow()).getHost(),
        httpsProxy.orElse(httpProxy.orElseThrow()).getPort()
    ));
  }
}
