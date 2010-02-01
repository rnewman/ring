(ns ring.middleware.session
  (:use ring.middleware.cookies
        ring.middleware.session.memory
        clojure.contrib.def)
  (:import java.util.UUID))

(defn wrap-session
  "Uses the read-session multimethod to read in a session map, wraps it in an
  atom, then adds it to the :session key of the request map. After the handler
  function has completed, the modified session atom is stored using the
  write-session multimethod."
  ([handler]
    (wrap-session handler {}))
  ([handler options]
    (let [store  (options :store (memory-store))
          cookie (options :cookie-name "ring-session")]
      (wrap-cookies
        (fn [request]
          (let [session-key (get-in request [:cookies cookie :value])
                session  (atom ((store :read) session-key))
                request  (assoc request :session session)
                response (handler request)
                new-session-key (if @session
                                  ((store :write) @session)
                                  ((store :delete) session-key))]
              (if (not= session-key new-session-key)
                (assoc response :cookies {cookie new-session-key})
                response)))))))
