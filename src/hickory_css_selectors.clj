(ns hickory-css-selectors
  "Convert CSS selectors into Hickory selectors."
  (:require [clojure.string :as cs]
            [hickory.core :as h]
            [hickory.select :as s]
            [instaparse.core :as p]
            [instaparse.transform :as pt]))

(def css-selector-parser
  "An incomplete and not very good parser for CSS selectors.
  https://drafts.csswg.org/selectors-3/#selectors"
  (p/parser
   "
    <S> = TOKEN (PATH S)*
    TOKEN = (ELEM ID? | CLASS+ | ID | ANY) CLASS? SPEC?
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
    <SPACE> = #'\\s+'
    "))

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

(comment
  ;; TODO how about some tests
  (p/parse css-selector-parser ".some-class")
  (p/parse css-selector-parser ".xyz[x]")
  (p/parse css-selector-parser ".xyz[x=y]")
  (p/parse css-selector-parser ".xyz[x_z='y']")
  (p/parse css-selector-parser ".xyz[4]")
  (p/parse css-selector-parser "div#id")
  (p/parse css-selector-parser "#xyz.bold")
  (p/parse css-selector-parser "h1.bold[title]")
  (p/parse css-selector-parser ".foo.bar.baz")
  (p/parse css-selector-parser ".foo.bar > #foo")
  (p/parse css-selector-parser ".foo.bar #foo")
  (p/parse css-selector-parser "body:nth-child(1)")
  (p/parse css-selector-parser ".foo.bar>#foo:nth-child(1)")
  (p/parse css-selector-parser "x > .y > #z a b > c")
  (p/parse css-selector-parser "x y z")
  (p/parse css-selector-parser "#readme > div.Box-body.p-6 > article > p:nth-child(19)")
  (p/parse css-selector-parser "html > body > div > li:nth-child(2)"))

(defn join-children
  "Takes a sequence of selectors and returns the sequence with child relations
  joined e.g. 'x > y > z a b c' -> '(x > y > z) a b c'"
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

(comment
  ;; TODO tests would be nice right
  (join-children (p/parse css-selector-parser "x"))
  (join-children (p/parse css-selector-parser "x y z"))
  (join-children
   (p/parse css-selector-parser "html > body div > li:nth-child(2)")
   list))

(defn parse-css-selector
  "Builds a Hickory selector from given CSS selector string."
  [s]
  (->> (p/parse css-selector-parser s)
       (pt/transform syntax->selector)
       (join-children)
       (apply s/descendant)))

(comment
  ;; TODO how about a test
  (def tree
    (-> "<html><body>
          <p class='some-class other-class'>
            some text</p>
          <div id='bar' aria-name='ya'><i>italia</i>
            <ul>
             <li></li>
             <li>foo</li>
            </ul>
          </div>
          <p>2</p>
          <span id='f'>g</span>
          <div id='another-div'></div>
         </body></html>"
        (h/parse)
        (h/as-hickory)))

  (s/select (parse-css-selector "div:nth-child(2) > i") tree)
  (s/select (parse-css-selector "div#bar") tree)
  (s/select (parse-css-selector "div[aria-name]") tree)
  (s/select (parse-css-selector "div[aria-namez]") tree)
  (s/select (parse-css-selector "div[aria-name^=y]") tree)
  (s/select (parse-css-selector "div[aria-name=yaz]") tree)
  (s/select (parse-css-selector "body > span[id$=f]") tree)
  (s/select (parse-css-selector "html>body>div li:nth-child(2)") tree)
  (s/select (parse-css-selector "body ul > li:nth-child(2)") tree)
  (s/select (parse-css-selector "html > body > div > li:nth-child(2)") tree)
  (s/select (parse-css-selector "div:nth-child(2) > i") tree))
