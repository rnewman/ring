(ns ring.middleware.cookies
  (:use clojure.contrib.def)
  (:use clojure.contrib.java-utils))

(defvar- re-token #"[!#$%&'*\-+.0-9A-Z\^_`a-z\|~]+"
  "HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068")

(defvar- re-quoted #"\"(\\\"|[^\"])*\""
  "HTTP quoted-string: <\"> *<any TEXT except \"> <\">. See RFC2068.")

(defvar- re-value (str re-token "|" re-quoted)
  "HTTP value: token | quoted-string. See RFC2109")

(defvar- re-cookie
  (re-pattern (str "\\s*(" re-token ")=(" re-value ")\\s*[;,]?"))
  "HTTP cookie-value: NAME \"=\" VALUE")

(defvar- cookie-attrs
  {"$Path" :path, "$Domain" :domain, "$Port" :port}
  "Special attributes defined by RFC2109 and RFC2965 that apply to the
  Cookie header.")

(defvar- set-cookie-attrs
  {:comment "Comment", :comment-url "CommentURL", :discard "Discard",
   :domain "Domain", :max-age "Max-Age", :path "Path", :port "Port",
   :secure "Secure", :version "Version"}
  "Attributes defined by RFC2109 and RFC2965 that apply to the Set-Cookie
  header.")

(defn- parse-cookie-header
  "Turn a HTTP Cookie header into a list of name/value pairs."
  [header]
  (for [[_ name value] (re-seq re-cookie header)]
    [name value]))

(defn- normalize-quoted-strs
  "Turn quoted strings into normal Clojure strings using read-string."
  [cookies]
  (for [[name value] cookies]
    (if (.startsWith #^String value "\"")
      [name (read-string value)]
      [name value])))

(defn- get-cookie
  "Get a single cookie from a sequence of cookie-values"
  [[[name value] & cookie-values]]
  {name (reduce
          (fn [m [k v]] (assoc m (cookie-attrs k) v))
          {:value value}
          (take-while (comp cookie-attrs first) cookie-values))})

(defn- to-cookie-map
  "Turn a sequence of cookie-values into a cookie map."
  [values]
  (loop [values values, cookie-map {}]
    (if (seq values)
      (let [cookie (get-cookie values)]
        (recur
          (drop (-> cookie first val count) values)
          (merge cookie-map cookie)))
        cookie-map)))

(defn- parse-cookies
  "Parse the cookies from a request map."
  [request]
  (if-let [cookie (get-in request [:headers "cookie"])]
    (-> cookie
      parse-cookie-header
      normalize-quoted-strs
      to-cookie-map
      (dissoc "$Version"))))

(defn- write-attr
  "Turn a name-value pair into a cookie attr string."
  [name value]
  (str (as-str name) "=" (pr-str value)))

(defn- write-attr-map
  "Write a map of cookie attributes to a string."
  [attrs]
  (for [[key value] attrs]
    (let [name (set-cookie-attrs key)]
      (cond
        (true? value)  (str ";" name)
        (false? value) ""
        :else          (str ";" (write-attr name value))))))

(defn- write-cookies
  "Turn a map of cookies into a seq of strings for a Set-Cookie header."
  [cookies]
  (for [[name value] cookies]
    (if (map? value)
      (apply str (write-attr name (:value value))
                 (write-attr-map (dissoc value :value)))
      (write-attr name value))))

(defn- set-cookies
  "Add a Set-Cookie header to a response if there is a :cookies key."
  [response]
  (if-let [cookies (:cookies response)]
    (assoc-in response 
              [:headers "Set-Cookie"]
              (write-cookies cookies))
    response))

(defn wrap-cookies
  "Parses the cookies in the request map, then assocs the resulting map
  to the :cookies key on the request."
  [handler]
  (fn [request]
    (let [request  (assoc request :cookies (parse-cookies request))
          response (handler request)]
      (-> response
        set-cookies
        (dissoc :cookies)))))
