(ns ring.middleware.session
  (:use ring.middleware.cookies
        clojure.contrib.def)
  (:import java.util.UUID))

;; Multimethods for defining session storage backends

(defmulti read-session
  "Returns a session map given a key and the map of options passed to the
  wrap-session middleware. If the session does not already exist, it should
  be created. The dispatch function is (:store options)."
  (fn [session-key options] (:store options))
  :default :memory)

(defmulti write-session
  "Write out a session map and return a key that can be used to read the
  session, given the map of options passed to the wrap-session middleware.
  If the session map is nil, the session should be deleted. The dispatch
  function is (:store options)."
  (fn [session options] (:store options))
  :default :memory)

;; Default in-memory session storage

(defvar- memory-sessions (atom {})
  "In-memory session storage.")

(defmethod read-session :memory
  [session-key _]
  (if-let [session (@memory-sessions session-key)]
    (assoc session ::id session-key)
    {::id (str (UUID/randomUUID))}))

(defmethod write-session :memory
  [session _]
  (swap! memory-sessions assoc (::id session) session)
  (::id session))

;; Main middleware function

(defn wrap-session
  "Uses the read-session multimethod to read in a session map, wraps it in an
  atom, then adds it to the :session key of the request map. After the handler
  function has completed, the modified session atom is stored using the
  write-session multimethod."
  ([handler]
    (wrap-session handler {:store :memory}))
  ([handler options]
    (let [cookie (options :cookie-name "ring-session")]
      (wrap-cookies
        (fn [request]
          (let [session-key (get-in request [:cookies cookie :value])
                session  (atom (read-session session-key options))
                request  (assoc request :session session)
                response (handler request)
                session-key (write-session @session options)]
              (assoc response :cookies {cookie session-key})))))))
