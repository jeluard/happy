# Happy [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html)

[Usage](#usage) | [Interceptor](#interceptor) | [Representor](#representor)

[![Clojars Project](http://clojars.org/happy/latest-version.svg)](http://clojars.org/happy).

A Clojure(Script) HTTP async client library with swappable implementation.

`happy` ships with a Clojure client based on [OkHTTP](http://square.github.io/okhttp/) and a ClojureScript client based on XMLHttpRequest.

## Usage

`happy` main function is `happy.core/send!`. It allows to send an HTTP call represented as a map and receive via a handler function a response map.

```clojure
(ns my.app
  (:require [happy.core :as h]
            [happy.client.xmlhttprequest :as hc]))

(h/set-default-client! (hc/create))

(let [c (h/send! {:method :get :url "http://google.com"} {:handler #(println "received " %)})]
  ; an HTTP call can be aborted
  (h/-abort c))
```

A request map accepts following keys:

* `:method` an uppercase String identifying the HTTP method used
* `:headers` a String/String map of key/value headers
* `:body` a body payload whose type must match client implementation capacities

When called, the `:handler` function will receive as single argument a response map accepting following keys:

* `:type` a keyword identifying the response: can be `:response`, `:progress` of `:failure`

Depending on the `:type` value others keys are present.

if `:type` is `:response`:

* `:status` an integer of the HTTP status code
* `:headers` a String/(String or seq) map of key/value headers
* `:body` a payload whose type depends on client implementation

if `:type` is `:progress`:

* `:type` a keyword identifying if this is a `:receiving` or `:sending` progress
* `:loaded` an integer of the count of currently loaded bytes, optional
* `:total` an integer of total bytes, optional

if `:type` is `:failure`:

* `:termination` a keyword whose value can be `:abort`, `:timeout` or `:network`
* `:reason` a String detailing the failure, optional

A handler is called only once per request with a `:type` of `:response` or `:failure`.
If `:report-progress?` option is provided and the client implementation supports it `:handler` can be called a number of times with type `:progress`.

For simplicity both request and response are modeled after the ring [SPEC](https://github.com/ring-clojure/ring/blob/master/SPEC).

Helper functions for common verbs are provided to simplify common calls.

```clojure
(ns my.app
  (:require [happy.core :as h :refer [GET PUT]]
            [happy.client.xmlhttprequest :as hc]))

(h/set-default-client! (hc/create))

(GET "http://google.com" {:handler #(println "received " %)})
(PUT "http://my-app.com" {:data "some payload"} {:handler #(println "received " %)})
```

### Options

The second parameter to `happy.core/send!` is a map of options that will affect an HTTP call.

Each client can accept any option. Those must be advertised in the `happy.core/-supports` map as `extra-options`.

Common options must available (optional unless specified):

* `:client` to define the client implementation, mandatory
* `:handler` the callback function called when the HTTP call is executed
* `:timeout` the maximum time allowed for the HTTP call to finish, in milliseconds
* `:request-body-as` the type of `:body` send by the client. Client specific, default to :string
* `:response-body-as` the type of `:body` received by the client. Client specific, default to :string
* `:report-progress?` if `:progress` event are provided to the callback `:handler`
* `:request-interceptors` the sequence of [interceptors](#interceptor) applied to a request
* `:response-interceptors` the sequence of [interceptors](#interceptor) applied to a response

Options can also be set globally (stored in `happy.core/default-options`) using `happy.core/swap-options!`, `happy.core/merge-options!` or `happy.core/set-default-client!`.

```clojure
(ns my.app
  (:require [happy.core :as h :refer [GET PUT]]
            [happy.client.xmlhttprequest :as hc]))

(h/set-default-client! (hc/create))
(h/merge-options! {:report-progress? true
                   :handler #(println %)})

(GET "http://google.com")
```

## Interceptor

Interceptors allow users to modify request and response part of an HTTP call. Interceptors are simple function returning their argument eventually modified and are applied in order.
A request interceptor is specified via `:request-interceptors` and receive as argument a sequence of the request map and the options map.
A response interceptor is specified via `:response-interceptors` and receive as argument the response map.

```clojure
(ns my.app
  (:require [happy.core :as h :refer [GET]]
            [happy.client.xmlhttprequest :as hc]))

(h/set-default-client! (hc/create))
(h/merge-options! {:request-interceptors [(fn [[req om :as m]] (println "Request: " m) m)]})

(GET "http://google.com")
```

## Representor

Representors encapsulate the logic of converting HTTP body between the user and the client implementation. Custom representors can be provided by implementing the `happy.core/Representor` protocol.

To have a representor used automatically as part of the HTTP call it must be defined as an interceptor using respectively the `request-interceptors` and `response-interceptors` options. `happy.representors/as-request-interceptor` and `happy.representors/as-response-interceptor` helps defining them.
Representors as interceptors are automatically applied based on request / response `content-type` and will replace `:body` with the result of their invocation.

Default representor for `json`, `transit`  and other common mime types are available and defined by default (see **src/happy/representor**).

## License

Copyright (C) 2015 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.