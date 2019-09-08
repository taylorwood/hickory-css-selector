(ns hickory-css-selectors-test
  (:require [clojure.test :refer :all]
            [hickory.core :as h]
            [hickory.select :as s]
            [hickory-css-selectors :refer :all]
            [instaparse.core :as p]))

(deftest css-selector-parser-test
  (are [css tree] (= tree (p/parse css-selector-parser css))
    ".some-class"
    [[:TOKEN [:CLASS "some-class"]]]
    ".xyz[x]"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "x"]]]
    ".xyz[x=y]"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "x" [:PRED [:ATTR_OP "="] [:VALUE "y"]]]]]
    ".xyz[x_z='y']"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "x_z" [:PRED [:ATTR_OP "="] [:VALUE "y"]]]]]
    ".xyz[4]"
    [[:TOKEN [:CLASS "xyz"] [:ATTR "4"]]]
    "div#id"
    [[:TOKEN [:ELEM "div"] [:ID "id"]]]
    "#xyz.bold"
    [[:TOKEN [:ID "xyz"] [:CLASS "bold"]]]
    "h1.bold[title]"
    [[:TOKEN [:ELEM "h1"] [:CLASS "bold"] [:ATTR "title"]]]
    ".foo.bar.baz"
    [[:TOKEN [:CLASS "foo"] [:CLASS "bar"] [:CLASS "baz"]]]
    ".foo.bar > #foo"
    [[:CHILD
      [:TOKEN [:CLASS "foo"] [:CLASS "bar"]]
      [:TOKEN [:ID "foo"]]]]
    ".foo.bar + #foo"
    [[:NEXT_SIBLING
      [:TOKEN [:CLASS "foo"] [:CLASS "bar"]]
      [:TOKEN [:ID "foo"]]]]
    ".foo.bar ~ #foo"
    [[:SIBLING
      [:TOKEN [:CLASS "foo"] [:CLASS "bar"]]
      [:TOKEN [:ID "foo"]]]]
    ".foo.bar #foo"
    [[:DESCENDANT
      [:TOKEN [:CLASS "foo"] [:CLASS "bar"]]
      [:TOKEN [:ID "foo"]]]]
    "body:nth-child(1)"
    [[:TOKEN [:ELEM "body"] [:NTH_CHILD [:NTH "1"]]]]
    "div:has(p)"
    [[:TOKEN [:ELEM "div"] [:HAS_CHILD [:TOKEN [:ELEM "p"]]]]]
    "div:contains('bla')"
    [[:TOKEN [:ELEM "div"] [:HAS_TEXT "bla"]]]
    ".foo.bar>#foo:nth-child(1)"
    [[:CHILD
      [:TOKEN [:CLASS "foo"] [:CLASS "bar"]]
      [:TOKEN [:ID "foo"] [:NTH_CHILD [:NTH "1"]]]]]
    "x > .y > #z a b > c"
    [[:CHILD
      [:DESCENDANT
       [:DESCENDANT
        [:CHILD
         [:CHILD
          [:TOKEN [:ELEM "x"]]
          [:TOKEN [:CLASS "y"]]]
         [:TOKEN [:ID "z"]]]
        [:TOKEN [:ELEM "a"]]]
       [:TOKEN [:ELEM "b"]]]
      [:TOKEN [:ELEM "c"]]]]
    "x y z"
    [[:DESCENDANT
      [:DESCENDANT
       [:TOKEN [:ELEM "x"]]
       [:TOKEN [:ELEM "y"]]]
      [:TOKEN [:ELEM "z"]]]]
    "#readme > div.Box-body.p-6 > article > p:nth-child(19)"
    [[:CHILD
      [:CHILD
       [:CHILD
        [:TOKEN [:ID "readme"]]
        [:TOKEN [:ELEM "div"] [:CLASS "Box-body"] [:CLASS "p-6"]]]
       [:TOKEN [:ELEM "article"]]]
      [:TOKEN [:ELEM "p"] [:NTH_CHILD [:NTH "19"]]]]]
    "html > body > div > li:nth-child(2)"
    [[:CHILD
      [:CHILD
       [:CHILD
        [:TOKEN [:ELEM "html"]]
        [:TOKEN [:ELEM "body"]]]
       [:TOKEN [:ELEM "div"]]]
      [:TOKEN [:ELEM "li"] [:NTH_CHILD [:NTH "2"]]]]]))

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
        <div id='another-div'><div id='child-div'>find this text</div></div>
       </body></html>"
      (h/parse)
      (h/as-hickory)))

(defn select-css [css] (s/select (parse-css-selector css) tree))

(deftest select-test
  (is (= "bar" (-> (select-css "div#bar") first :attrs :id)))
  (is (= ["italia"] (-> (select-css "div:nth-child(2) > i") first :content)))
  (is (= ["foo"] (-> (select-css "body ul > li:nth-child(2)") first :content)))
  (is (= ["2"] (-> (select-css "div#bar + p") first :content)))
  (is (= ["bar" "another-div"] (map (comp :id :attrs) (select-css "p.some-class ~ div"))))
  (is (= "another-div" (-> (select-css "div:has(div)") first :attrs :id)))
  (is (= "child-div" (-> (select-css "div:contains('find this')") first :attrs :id)))
  (is (= "child-div" (-> (select-css "div:contains(\"find this\")") first :attrs :id)))
  (is (= "bar" (-> (select-css "*:has(*:has(*:contains('foo')))") first :attrs :id)))
  (is (= ["find this text"] (-> (select-css "span + div > div") first :content))))

(comment
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
  (s/select (parse-css-selector "div:nth-child(2) > i") tree)
  (s/select (parse-css-selector "div + p") tree)
  (s/select (parse-css-selector "div ~ p") tree)
  (s/select (parse-css-selector "div:has(div > p)") tree)
  (s/select (parse-css-selector "div:contains('bla! bla*')") tree))
