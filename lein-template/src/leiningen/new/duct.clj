(ns leiningen.new.duct
  (:require [leiningen.core.main :as main]
            [leiningen.new.templates :refer [->files]]
            [leiningen.new.profiles :as profiles]
            [leiningen.new.external-profiles :as external-profiles]
            [rewrite-clj.zip :as z]
            [clojure.java.io :as io]))

(defn insert-new-deps [deps-to-insert]
  (let [data (z/of-string (slurp (io/resource "project.clj")))
        prj-map (z/find-value data z/next 'defproject)
        deps (-> prj-map (z/find-value :dependencies) (z/right))]
    (-> deps (z/edit concat deps-to-insert) (z/root-string))))

(defn duct
  "Create a new Duct web application.

Accepts the following profile hints:
  +api      - adds API middleware and handlers
  +ataraxy  - adds the Ataraxy router
  +cljs     - adds in ClojureScript compilation and hot-loading
  +example  - adds an example handler
  +heroku   - adds configuration for deploying to Heroku
  +postgres - adds a PostgreSQL dependency and database component
  +site     - adds site middleware, a favicon, webjars and more
  +sqlite   - adds a SQLite dependency and database component"
  [name & hints]
  (when (.startsWith name "+")
    (main/abort "Failed to create project: no project name specified."))
  (main/info (str "Generating a new Duct project named " name "..."))
  (let [[native external] (profiles/group-profiles hints)
        mods  (cons :base native)
        data  (reduce into {} (map #(profiles/profile-data % name) mods))
        files (reduce into [] (map #(profiles/profile-files % data) mods))
        {:keys [extra-deps extra-files]} (external-profiles/main external data)]
    (main/info "EXTRA DEPS:")
    (main/info extra-deps)
    (main/info (str "Mods: " mods))
    (main/info "NEW FILES:")
    (main/info (map first extra-files))
    (apply ->files data (concat files
                                extra-files))
    (spit "foobar/project.clj" (insert-new-deps extra-deps)))
  (main/info "Run 'lein duct setup' in the project directory to create local config files."))
