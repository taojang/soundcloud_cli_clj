(ns soundcloud-cli-clj.api
  (:require [clj-http.client :as client]
            [taoensso.timbre :as timbre
             :refer (log error info warn fatal)]))

(def base-url "https://api.soundcloud.com")

(def base-url-v2 "https://api-v2.soundcloud.com")

(defn get-token
  [c-id c-sec uname pass]
  (try
    (-> (client/post (str base-url "/oauth2/token")
                 {:form-params {:client_id c-id
                                :client_secret c-sec
                                :username uname
                                :password pass
                                :scope "non-expiring"
                                :grant_type "password"}
                  :as :json})
        :body
        :access_token)
    (catch Exception ex
      (error ex "Error during getting token"))))

(defn get-user
  [c-id u-id]
  (try
    (:body (client/get (str base-url "/users/" u-id ".json")
                       {:query-params {:client_id c-id}
                        :as :json}))
    (catch Exception ex
      (error ex (str "Error during getting user: uid " u-id)))))

(defn get-stream
  [token & offset]
  (try
    (:body (client/get (str base-url "/me/activities/tracks/affiliated")
                {:query-params {:oauth_token token}
                 :as :json}))
    (catch Exception ex
      (error ex "Error during getting stream"))))
