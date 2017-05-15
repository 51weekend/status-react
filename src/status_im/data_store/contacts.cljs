(ns status-im.data-store.contacts
  (:require [status-im.data-store.realm.contacts :as data-store])
  (:refer-clojure :exclude [exists?]))

(defn- command->map-item
  [[_ {:keys [name] :as command}]]
  [(keyword name) command])

(defn- commands-map->commands-list
  [commands-map]
  (or (if (and commands-map (map? commands-map))
        (vals commands-map)
        commands-map)
      '()))

(defn get-all
  []
  (map
    (fn [{:keys [commands responses networks] :as contact}]
      (assoc contact
        :commands (into {} (map command->map-item commands))
        :responses (into {} (map command->map-item responses))
        :networks (map :value (vals networks))))
    (data-store/get-all-as-list)))

(defn get-by-id
  [whisper-identity]
  (update (data-store/get-by-id-cljs whisper-identity) :networks #(map :value %)))

(defn save
  [{:keys [whisper-identity pending?] :as contact}]
  (let [{pending-db? :pending?
         :as         contact-db} (data-store/get-by-id whisper-identity)
        contact' (-> contact
                     (assoc :pending? (boolean (if contact-db
                                                 (if (nil? pending?) pending-db? pending?)
                                                 pending?)))
                     (update :networks (fn [networks] (into [] (map #(hash-map :value %) networks))))
                     (update :commands commands-map->commands-list)
                     (update :responses commands-map->commands-list))]
    (data-store/save contact' (boolean contact-db))))

(defn save-all
  [contacts]
  (mapv save contacts))

(defn delete [contact]
  (data-store/delete contact))

(defn exists?
  [whisper-identity]
  (data-store/exists? whisper-identity))
