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
   "<selector> = token | path
    token = (elem id? | class | id | any) class* spec?
    class = <'.'> name
    id = <'#'> name
    any = <'*'>
    elem = name
    <name> = #'[A-Za-z0-9\\-\\_]+'
    <spec> = attr | nth-child | has-child | has-text
    attr = <'['> attr-body <']'>
    <attr-body> = name pred?
    pred = attr-op value
    attr-op = '=' | '!=' | '~=' | '^=' | '$='
    value = <'\"'> sym <'\"'> | <#'\\''> sym <#'\\''> | sym
    <sym> = #'[^\"\\'\\]]+'\n
    nth-child = <':nth-child('> nth <')'>
    nth = #'[0-9]+'
    has-child = <':has('> selector <')'>
    has-text = <':contains(\\''> text-squot <'\\')'> | <':contains(\"'> text-dquot <'\")'>
    <text-squot> = #'[^\\']*'
    <text-dquot> = #'[^\"]*'
    <path> = child | descendant | sibling | next-sibling
    child        = selector <space?> <'>'> <space?> token
    descendant   = selector <space> token
    sibling      = selector <space?> <'~'> <space?> token
    next-sibling = selector <space?> <'+'> <space?> token
    <space> = #'\\s+'"))

(def syntax->selector
  "Map from parser tokens to Hickory selectors."
  {:token        (fn [& ts] (apply s/and ts))
   :class        s/class
   :id           s/id
   :any          (constantly s/any)
   :elem         s/tag
   :attr         (fn [attr & [pred]] (if pred (s/attr attr pred) (s/attr attr)))
   :pred         (fn [op [_ val]] #(op % val))
   :attr-op      #(case %
                    "=" =
                    "!=" not=
                    "^=" cs/starts-with?
                    "$=" cs/ends-with?
                    "~=" cs/includes?)
   :nth-child    s/nth-child
   :nth          #(Integer/parseInt %)
   :has-child    s/has-child
   :has-text     (comp s/find-in-text re-pattern)
   :child        s/child
   :descendant   s/descendant
   :sibling      s/follow
   :next-sibling s/follow-adjacent})

(defn parse-css-selector
  "Builds a Hickory selector from given CSS selector string."
  [s]
  (->> (p/parse css-selector-parser s)
       (pt/transform syntax->selector)
       (first)))
