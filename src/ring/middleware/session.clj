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
  The dispatch function is (:store options)."
  (fn [session options] (:store options))
  :default :memory)

(defmulti delete-session
  "Given a session key, remove the associated session entirely from storage
  (if possible), and either return a new session key or nil."
  (fn [session-key options] (:store options))
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
  (if session
    (swap! memory-sessions assoc (::id session) session)
    (swap! memory-sessions dissoc (::id session)))
  (::id session))

(defmethod delete-session :memory
  [session-key _]
  (swap! memory-sessions dissoc session-key)
  nil)

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
                new-session-key (if @session
                                  (write-session @session options)
                                  (delete-session session-key options))]
              (if (not= session-key new-session-key)
                (assoc response :cookies {cookie new-session-key})
                response)))))))
