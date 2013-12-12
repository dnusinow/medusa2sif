(ns medusa2sif.core
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [clojure.java.io :as jio]
            [clojure.tools.cli :refer [parse-opts]])
  (:use (clojure.java.io))
  (:gen-class :main true))


(defn line-ok?
  [line]
  (not (or
        (nil? line)
        (= (string/trim line) ""))))

(defn is-section?
  [line]
  (let [match (re-find #"^\*" line)]
    (not (nil? match))))

(defn parse-section-line
  [cur-section line]
  (let [match (re-find #"^\*.*" line)]
    (if (not (nil? match))
      (subs match 1)
      cur-section)))

(defn get-section
  [last-section line]
  (if (and (or (nil? last-section)
               (is-section? line))
           (line-ok? line))
    (parse-section-line last-section line)
    last-section))

(defn parse-node-line 
  [line data]
  (let [rec (string/split line #"\t")]
    {:nodes (conj (:nodes data)
                  {:name (first rec)
                   :x (second rec)
                   :y (nth rec 2)
                   :rgb (string/replace-first (nth rec 3) #"^c " "")  
                   :description (nth rec 4) })
     :edges (:edges data) }))

;; TODO: Parse the confidence and interaction # better.
(defn parse-edge-line
  [line data]
  (let [rec (string/split line #"\t")
        sub-rec (string/split (nth rec 2) #"\s+")]
    {:nodes (:nodes data)
     :edges (conj (:edges data)
                  {:from (first rec)
                   :to (second rec) }) }))

(defn parse-line
  [line cur-section data]
  (if (and (line-ok? line)
           (not (is-section? line)))
    (if (= cur-section "nodes")
      (parse-node-line line data)
      (parse-edge-line line data))
    data))

(defn parse-lines
  [fseq last-section data]
  (if (empty? fseq)
    data
    (let [line (first fseq)
          cur-section (get-section last-section line)]
      (recur (rest fseq) cur-section 
             (parse-line line cur-section data)))))

(defn parse-medusa 
  [inpath]
  (with-open [rdr (jio/reader inpath)]
    (parse-lines (line-seq rdr) nil {:nodes nil :edges nil})))

(defn edge-nodes
  [data]
  (set (reduce (fn [accum new] 
                 (concat accum (list (:from new) (:to new))))
               '() (:edges data))))

(defn all-nodes
  [data]
  (set (set/union (edge-nodes data)
                  (reduce (fn [accum new]
                            (concat accum (list (:name new))))
                          '() (:nodes data)))))

(defn nodes-with-no-edge
  [data]
  (sort (set/difference (all-nodes data) (edge-nodes data))))

(defn edges-to-edge-list
  [data interaction]
  (distinct (reduce (fn [accum new]
            (conj accum 
                  (list (:from new) 
                        interaction
                        (:to new))))
                    '() (:edges data))))

(defn network-to-sif
  [data interaction]
  (concat (map (fn [x] (string/join "\t" x))
               (edges-to-edge-list data interaction))
          (nodes-with-no-edge data)))

;; UI and -main
(def cli-options
  [[:id :infile,
    :short-opt "-i",
    :long-opt "--in", 
    :required "MEDUSA",
    :desc "The path to the medusa file to be converted to SIF"]
   
   [:id :outfile,
    :short-opt "-o",
    :long-opt "--out",
    :required "SIF",
    :desc "The path to the SIF file to be written"]

   [:id :interaction,
    :short-opt "-t",
    :long-opt "--type",
    :required "INTERACTION",
    :default "MEDUSA",
    :desc "The type of interaction for network edges"]

   ["-h" "--help"]])

(defn usage [options-summary]
  (->> [""
        "medusa2sif - converts a medusa network file in to a sif file"
        ""
        "Usage: java -jar medusa2sif.jar [-i] in.medusa [-o] out.sif"
        ""
        "Arguments"
        "---------"
        "-i/--input - path to input Medusa file"
        "-o/--output - path to output SIF file"
        "-t/--type - interaction type for edges"
        ""
        "Arguments may be specified using the command switches or positionally, but not a mix."
        ""]
       (string/join \newline)))

;; TODO: Let this function handle mix of switches and positional args
(defn good-file-opts 
  [options args]
  (let [in-path (or (:infile options) (first args))
        out-path (or (:outfile options) (second args))]
    (if (or (nil? in-path) (nil? out-path))
      nil
      [in-path out-path])))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        ;; parse-args doesn't return anything remaining if there's no
        ;; command line options, so we need to use the args from main
        ;; in that case
        file-paths (good-file-opts options (if (empty? arguments) args arguments))]

    (cond (:help options) (exit 0 (usage summary))
          (nil? file-paths) (exit 1 (usage summary)))

    (print "Starting conversion... ")
    (with-open [fout (jio/writer (second file-paths))]
      (doseq [out-dat (network-to-sif (parse-medusa (first file-paths)) (:interaction options))]
        (.write fout (str out-dat "\n"))))
    (println "done.")))

;; (-main "./test/medusa2sif/network_medusa.AQipq2oBdIZy.dat" "foo.sif")
;; (good-file-opts (:options foo) (:arguments foo))
