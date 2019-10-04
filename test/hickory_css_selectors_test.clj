(ns hickory-css-selectors-test
  (:require [clojure.test :refer :all]
            [hickory.core :as h]
            [hickory.select :as s]
            [hickory-css-selectors :refer :all]
            [instaparse.core :as p]))

(deftest css-selector-parser-test
  (are [css tree] (= tree (p/parse css-selector-parser css))
    ".some-class"
    [[:token [:class "some-class"]]]
    ".xyz[x]"
    [[:token [:class "xyz"] [:attr "x"]]]
    ".xyz[x=y]"
    [[:token [:class "xyz"] [:attr "x" [:pred [:attr-op "="] [:value "y"]]]]]
    ".xyz[x_z='y']"
    [[:token [:class "xyz"] [:attr "x_z" [:pred [:attr-op "="] [:value "y"]]]]]
    ".xyz[4]"
    [[:token [:class "xyz"] [:attr "4"]]]
    "div#id"
    [[:token [:elem "div"] [:id "id"]]]
    "#xyz.bold"
    [[:token [:id "xyz"] [:class "bold"]]]
    "h1.bold[title]"
    [[:token [:elem "h1"] [:class "bold"] [:attr "title"]]]
    ".foo.bar.baz"
    [[:token [:class "foo"] [:class "bar"] [:class "baz"]]]
    ".foo.bar > #foo"
    [[:child
      [:token [:class "foo"] [:class "bar"]]
      [:token [:id "foo"]]]]
    ".foo.bar + #foo"
    [[:next-sibling
      [:token [:class "foo"] [:class "bar"]]
      [:token [:id "foo"]]]]
    ".foo.bar ~ #foo"
    [[:sibling
      [:token [:class "foo"] [:class "bar"]]
      [:token [:id "foo"]]]]
    ".foo.bar #foo"
    [[:descendant
      [:token [:class "foo"] [:class "bar"]]
      [:token [:id "foo"]]]]
    "body:nth-child(1)"
    [[:token [:elem "body"] [:nth-child [:nth "1"]]]]
    "div:has(p)"
    [[:token [:elem "div"] [:has-child [:token [:elem "p"]]]]]
    "div:contains('bla')"
    [[:token [:elem "div"] [:has-text "bla"]]]
    ".foo.bar>#foo:nth-child(1)"
    [[:child
      [:token [:class "foo"] [:class "bar"]]
      [:token [:id "foo"] [:nth-child [:nth "1"]]]]]
    "x > .y > #z a b > c"
    [[:child
      [:descendant
       [:descendant
        [:child
         [:child
          [:token [:elem "x"]]
          [:token [:class "y"]]]
         [:token [:id "z"]]]
        [:token [:elem "a"]]]
       [:token [:elem "b"]]]
      [:token [:elem "c"]]]]
    "x y z"
    [[:descendant
      [:descendant
       [:token [:elem "x"]]
       [:token [:elem "y"]]]
      [:token [:elem "z"]]]]
    "#readme > div.Box-body.p-6 > article > p:nth-child(19)"
    [[:child
      [:child
       [:child
        [:token [:id "readme"]]
        [:token [:elem "div"] [:class "Box-body"] [:class "p-6"]]]
       [:token [:elem "article"]]]
      [:token [:elem "p"] [:nth-child [:nth "19"]]]]]
    "html > body > div > li:nth-child(2)"
    [[:child
      [:child
       [:child
        [:token [:elem "html"]]
        [:token [:elem "body"]]]
       [:token [:elem "div"]]]
      [:token [:elem "li"] [:nth-child [:nth "2"]]]]]))

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
