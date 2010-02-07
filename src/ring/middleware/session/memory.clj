(ns ring.middleware.session.memory
  "In-memory session storage."
  (:use clojure.contrib.def)
  (:import java.util.UUID))

(defvar- session-map (atom {})
  "Atom in which to keep the in-memory sessions.")

(defn memory-store
  "Creates an in-memory session storage engine."
  []
  {:read (fn [session-key]
           (if-let [session (@session-map session-key)]
             (assoc session ::id session-key)
             {::id (str (UUID/randomUUID))}))
   :write (fn [session]
            (swap! session-map assoc (::id session) session)
            (::id session))
   :delete (fn [session-key]
             (swap! session-map dissoc session-key)
             nil)})
