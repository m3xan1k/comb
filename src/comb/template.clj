(ns comb.template
  "Clojure templating library."
  (:refer-clojure :exclude [fn eval])
  (:require [clojure.core :as core]))

(defn- read-source [source]
  (if (string? source)
    source
    (slurp source)))

(defn html-escape
  "Escape HTML entities to prevent XSS attacks."
  [s]
  (if (nil? s)
    nil
    (let [s (str s)]
      (-> s
          (.replace "&" "&amp;")
          (.replace "<" "&lt;")
          (.replace ">" "&gt;")
          (.replace "\"" "&quot;")
          (.replace "'" "&#x27;")))))

(def delimiters ["<%" "%>"])

(def parser-regex
  (re-pattern
   (str "(?s)\\A"
        "(?:" "(.*?)"
        (first delimiters) "(.*?)" (last delimiters)
        ")?"
        "(.*)\\z")))

(defn emit-string [s]
  (print "(print " (pr-str s) ")"))

(defn emit-expr [expr]
  (cond
    (.startsWith expr "==")  ; <%== %> for raw output
    (print "(print " (subs expr 2) ")")
    
    (.startsWith expr "=")   ; <%= %> for escaped output (default)
    (print "(print (comb.template/html-escape " (subs expr 1) "))")
    
    :else                    ; <% %> for code blocks
    (print expr)))

(defn- parse-string [src]
  (with-out-str
    (print "(do ")
    (loop [src src]
      (let [[_ before expr after] (re-matches parser-regex src)]
        (if expr
          (do (emit-string before)
              (emit-expr expr)
              (recur after))
          (do (emit-string after)
              (print ")")))))))

(defn compile-fn [args src]
  (core/eval
   `(core/fn ~args
      (with-out-str
        ~(-> src read-source parse-string read-string)))))

(defmacro fn
  "Compile a template into a function that takes the supplied arguments. The
  template source may be a string, or an I/O source such as a File, Reader or
  InputStream."
  {:clj-kondo/lint-as 'clojure.core/fn
   :clj-kondo/ignore [:unused-binding]}
  [args source]
  `(compile-fn '~args ~source))

(defn eval
  "Evaluate a template using the supplied bindings. The template source may
  be a string, or an I/O source such as a File, Reader or InputStream."
  ([source]
     (eval source {}))
  ([source bindings]
     (let [keys (map (comp symbol name) (keys bindings))
           func (compile-fn [{:keys (vec keys)}] source)]
       (func bindings))))
