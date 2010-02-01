(ns ring.middleware.session
  (:use ring.middleware.cookies
        ring.middleware.session.memory
        clojure.contrib.def)
  (:import java.util.UUID))

(defn wrap-session
  "Creates an atom to hold a mutable session map, and adds this to the
  :session key on the request map. Any changes made to the map will be
  persisted when the handler ends.

  The following options are available:
    :store
      An implementation map containing :read, :write, and :delete
      keys. This determines how the session is stored. Defaults to
      in-memory storage.
    :cookie-name
      The name of the cookie that holds the session key. Defaults to
      \"ring-session\""
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
