# Hickory CSS Selectors

[Hickory](https://github.com/davidsantiago/hickory) provides a namespace of [_selectors_](https://github.com/davidsantiago/hickory#selectors)
with functionality similar to CSS selectors, for selecting particular elements from a document tree.

This proof-of-concept provides a CSS-to-Hickory selector translator that can turn
a CSS selector like this:
```
div#main > ul.styled a[href]
```
into a Hickory selector like this:
```clojure
(s/descendant
  (s/child
    (s/and (s/tag :div) (s/id :main))
    (s/and (s/tag :ul) (s/class :styled)))
  (s/and (s/tag :a) (s/attr :href)))
```

## Usage

Add a dependency:
```clojure
{:deps {hickory-css-selector
        {:git/url "https://github.com/taylorwood/hickory-css-selector"}}}
```

Convert some HTML into Hickory, then select elements from it using CSS selector syntax:
```clojure
(require '(hickory [core :as h] [select :as s]))
(def doc (h/as-hickory (h/parse (slurp "https://clojure.org"))))

(use 'hickory-css-selectors)
(s/select (parse-css-selector "a[href~=reference]") doc)
=>
[{:type :element,
  :attrs
  {:href "/reference/documentation", :class "w-nav-link clj-nav-link"},
  :tag :a,
  :content ["Reference‍"]}]
```

## Contributions

I welcome them.
