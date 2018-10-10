(ns hickory-css-selectors
  "Convert CSS selectors into Hickory selectors."
  (:require [clojure.string :as cs]
            [hickory.select :as s]
            [instaparse.core :as p]
            [instaparse.transform :as pt]))

(def css-selector-parser
  "An incomplete and not very good parser for CSS selectors.
  https://drafts.csswg.org/selectors-3/#selectors"
  (p/parser
   "<S> = TOKEN (PATH S)*
    TOKEN = (ELEM ID? | CLASS | ID | ANY) CLASS* SPEC?
    CLASS = <'.'> NAME
    ID = <'#'> NAME
    ANY = <'*'>
    ELEM = NAME
    <SPEC> = ATTR | NTH_CHILD
    NTH_CHILD = <':nth-child('> NTH <')'>
    ATTR = <'['> ATTR_BODY <']'>
    <ATTR_BODY> = NAME PRED?
    NTH = #'[0-9]+'
    PRED = ATTR_OP VALUE
    ATTR_OP = '=' | '!=' | '~=' | '^=' | '$='
    VALUE = <'\"'> SYM <'\"'> | <#'\\''> SYM <#'\\''> | SYM
    <SYM> = #'[^\"\\'\\]]+'
    <NAME> = #'[A-Za-z0-9\\-\\_]+'
    <PATH> = CHILD | DESCENDANT
    CHILD = <SPACE?> <'>'> <SPACE?>
    <DESCENDANT> = <' '>
    <SPACE> = #'\\s+'"))

(def syntax->selector
  "Map from parser tokens to Hickory selectors."
  {:TOKEN     (fn [& ts] (apply s/and ts))
   :CLASS     s/class
   :ID        s/id
   :ANY       (constantly s/any)
   :ELEM      s/tag
   :ATTR      (fn [attr & [pred]] (if pred (s/attr attr pred) (s/attr attr)))
   :PRED      (fn [op [_ val]] #(op % val))
   :ATTR_OP   #(case %
                 "=" =
                 "!=" not=
                 "^=" cs/starts-with?
                 "$=" cs/ends-with?
                 "~=" cs/includes?)
   :NTH_CHILD s/nth-child
   :NTH       #(Integer/parseInt %)})

(defn join-children
  "Takes a sequence of selectors and returns it with child relations joined
  e.g. 'x y > z a > b c' -> 'x (y > z) (a > b) c'"
  ([tree] (join-children tree s/child))
  ([tree join]
   (loop [[curr & [next & more :as tail]] tree
          children []
          out []]
     (cond
       (not curr)        out
       (= [:CHILD] next) (recur more (conj children curr) out)
       (seq children)    (recur tail [] (conj out (apply join (conj children curr))))
       :else             (recur tail children (conj out curr))))))

(defn parse-css-selector
  "Builds a Hickory selector from given CSS selector string."
  [s]
  (->> (p/parse css-selector-parser s)
       (pt/transform syntax->selector)
       (join-children)
       (apply s/descendant)))
