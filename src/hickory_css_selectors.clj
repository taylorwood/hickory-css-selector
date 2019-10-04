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
   "<SELECTOR> = TOKEN | PATH
    TOKEN = (ELEM ID? | CLASS | ID | ANY) CLASS* SPEC?
    CLASS = <'.'> NAME
    ID = <'#'> NAME
    ANY = <'*'>
    ELEM = NAME
    <NAME> = #'[A-Za-z0-9\\-\\_]+'
    <SPEC> = ATTR | NTH_CHILD | HAS_CHILD | HAS_TEXT
    ATTR = <'['> ATTR_BODY <']'>
    <ATTR_BODY> = NAME PRED?
    PRED = ATTR_OP VALUE
    ATTR_OP = '=' | '!=' | '~=' | '^=' | '$='
    VALUE = <'\"'> SYM <'\"'> | <#'\\''> SYM <#'\\''> | SYM
    <SYM> = #'[^\"\\'\\]]+'\n
    NTH_CHILD = <':nth-child('> NTH <')'>
    NTH = #'[0-9]+'
    HAS_CHILD = <':has('> SELECTOR <')'>
    HAS_TEXT = <':contains(\\''> TEXT_SQ <'\\')'> | <':contains(\"'> TEXT_DQ <'\")'>
    <TEXT_SQ> = #'[^\\']*'
    <TEXT_DQ> = #'[^\"]*'
    <PATH> = CHILD | DESCENDANT | SIBLING | NEXT_SIBLING
    CHILD        = SELECTOR <SPACE?> <'>'> <SPACE?> TOKEN
    DESCENDANT   = SELECTOR <SPACE> TOKEN
    SIBLING      = SELECTOR <SPACE?> <'~'> <SPACE?> TOKEN
    NEXT_SIBLING = SELECTOR <SPACE?> <'+'> <SPACE?> TOKEN
    <SPACE> = #'\\s+'"))

(def syntax->selector
  "Map from parser tokens to Hickory selectors."
  {:TOKEN         (fn [& ts] (apply s/and ts))
   :CLASS         s/class
   :ID            s/id
   :ANY           (constantly s/any)
   :ELEM          s/tag
   :ATTR          (fn [attr & [pred]] (if pred (s/attr attr pred) (s/attr attr)))
   :PRED          (fn [op [_ val]] #(op % val))
   :ATTR_OP       #(case %
                     "=" =
                     "!=" not=
                     "^=" cs/starts-with?
                     "$=" cs/ends-with?
                     "~=" cs/includes?)
   :NTH_CHILD     s/nth-child
   :NTH           #(Integer/parseInt %)
   :HAS_CHILD     s/has-child
   :HAS_TEXT      (comp s/find-in-text re-pattern)
   :CHILD         s/child
   :DESCENDANT    s/descendant
   :SIBLING       s/follow
   :NEXT_SIBLING  s/follow-adjacent})

(defn parse-css-selector
  "Builds a Hickory selector from given CSS selector string."
  [s]
  (->> (p/parse css-selector-parser s)
       (pt/transform syntax->selector)
       (first)))
