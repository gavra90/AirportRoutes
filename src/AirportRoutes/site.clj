(ns AirportRoutes.site
(:use (compojure handler 
                 [core :only (GET POST defroutes)]))
(:require compojure.route 
          [AirportRoutes.core :as dajks]
          [net.cgrand.enlive-html :as en]
          [ring.util.response :as response] 
          [ring.adapter.jetty :as jetty]
          [clj-json.core :as json]))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})
(def airpor-data dajks/data)

(defn id-to-xy [solution] 
  (reduce (fn [acc e]              
            (conj acc  (str (:logitude (airpor-data e)) "*" (:lantitude (airpor-data e)) "*"(:name (airpor-data e)))))
          [] solution))

(defn data-for-combo [airpor-data]
  (reduce (fn [acc [id m]]
            (assoc acc id (str  (:name m)" " (:city m)" " (:country m))))
          {0 "Select airport"} airpor-data))

(def combo-data (data-for-combo airpor-data))

(defn map-cordinates [id-form id-to]
  "calculate coordinates for google maps"
  (let[maxx (max (:lantitude (airpor-data id-form)(:lantitude (airpor-data id-to))))
       maxy (max (:logitude (airpor-data id-form)(:logitude (airpor-data id-to))))
       minx (min (:lantitude (airpor-data id-to) (:lantitude (airpor-data id-form))))
       miny (min (:logitude (airpor-data id-to)(:logitude (airpor-data id-form))))
       dx (- (java.lang.Math/abs maxx) (java.lang.Math/abs minx))
       dy (- (java.lang.Math/abs maxy) (java.lang.Math/abs miny))
       cx (+ minx (/ dx 2))
       cy (+ miny (/ dy 2))]
    (str  cx "*" cy)))

(en/deftemplate homepage
  (en/xml-resource "homepage.html")
  [request]
   [:#from :option] (en/clone-for[[v t] combo-data]
                          [:option]
                          (en/content t)
                          [:option]
                          (en/set-attr :value (str v)))
    [:#to :option] (en/clone-for[[v t] combo-data]
                          [:option]
                          (en/content t)
                          [:option]
                          (en/set-attr :value (str v))))


(defroutes app*
  (compojure.route/resources "/")
  (POST "/callDajkst" request      
        (let [odA (-> request :params :odA)
              doB (-> request :params :doB)
              range (-> request :params :txtRange)
              solution (conj (id-to-xy (dajks/dajks-alg 
                                        (java.lang.Integer/parseInt odA) 
                                        (java.lang.Integer/parseInt doB)
                                        (java.lang.Integer/parseInt range)))
                            (map-cordinates 
                              (java.lang.Integer/parseInt odA) 
                              (java.lang.Integer/parseInt doB)))]
          (json-response  solution)))
  (GET "/" request (homepage request))
  (compojure.route/not-found "ERROR"))







(def app (compojure.handler/site app*))


(def server (jetty/run-jetty #'app {:port 8088 :join false}))


