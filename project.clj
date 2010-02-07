(defproject org.clojars.rnewman/ring "0.2.1-sessions"
  :description "A Clojure web applications library."
  :url "http://github.com/mmcgrana/ring"
  :dependencies [[org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0-master-SNAPSHOT"]
                 [org.mortbay.jetty/jetty "6.1.14"]
                 [org.mortbay.jetty/jetty-util "6.1.14"]
                 [org.mortbay.jetty/servlet-api-2.5 "6.1.14"]
                 [org.apache.httpcomponents/httpcore "4.0.1"]
                 [org.apache.httpcomponents/httpcore-nio "4.0.1"]
                 [clj-html "0.1.0-SNAPSHOT"]
                 [clj-stacktrace "0.1.0-SNAPSHOT"]]
  :repositories [["mvnrepository" "http://mvnrepository.com/"]
                 ["clojure-releases" "http://build.clojure.org/releases"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]])
