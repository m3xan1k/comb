(ns comb.test.template
  (:require [clojure.test :refer [deftest is]]
            [comb.template :as t]))

(deftest eval-test
  (is (= (t/eval "foo") "foo"))
  (is (= (t/eval "<%= 10 %>") "10"))
  (is (= (t/eval "<%= x %>" {:x "foo"}) "foo"))
  (is (= (t/eval "<%=x%>" {:x "foo"}) "foo"))
  (is (= (t/eval "<% (doseq [x xs] %>foo<%= x %> <% ) %>" {:xs [1 2 3]})
         "foo1 foo2 foo3 ")))

(deftest fn-test
  (is (= ((t/fn [x] "foo<%= x %>") "bar")
         "foobar")))

(deftest html-escaping-test
  (is (= (t/eval "<%= x %>" {:x "<script>"}) "&lt;script&gt;"))
  (is (= (t/eval "<%== x %>" {:x "<script>"}) "<script>"))
  (is (= (t/eval "<%= x %>" {:x "Hello & World"}) "Hello &amp; World"))
  (is (= (t/eval "<%= x %>" {:x "a\"b'c"}) "a&quot;b&#x27;c"))
  (is (= (t/eval "<%= x %>" {:x nil}) "nil"))
  (is (= (t/eval "<%= x %>" {:x 42}) "42")))

(deftest xss-prevention-test
  (is (= (t/eval "<%= x %>" {:x "<script>alert('XSS')</script>"})
         "&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;/script&gt;"))
  (is (= (t/eval "<%= x %>" {:x "javascript:alert(1)"})
         "javascript:alert(1)"))
  (is (= (t/eval "<a href='<%= x %>'>link</a>" {:x "javascript:alert(1)"})
         "<a href='javascript:alert(1)'>link</a>")))

(deftest backward-compatibility-test
  ;; Ensure existing behavior with raw output syntax
  (is (= (t/eval "<%== x %>" {:x "<b>bold</b>"}) "<b>bold</b>"))
  ;; Numbers and nil should work as before
  (is (= (t/eval "<%= x %>" {:x 42}) "42"))
  (is (= (t/eval "<%= x %>" {:x nil}) "nil"))
  ;; Complex expressions should work
  (is (= (t/eval "<%= (str x y) %>" {:x "Hello" :y " World"}) "Hello World")))

(deftest complex-template-escaping-test
  (is (= (t/eval "<ul><% (doseq [x items] %><li><%= x %></li><% ) %></ul>" 
                {:items ["<script>" "Hello & World" "a'b\"c"]})
         "<ul><li>&lt;script&gt;</li><li>Hello &amp; World</li><li>a&#x27;b&quot;c</li></ul>")))

(deftest escaping-performance-test
  (let [large-string (apply str (repeat 100 "<>&\"'"))]
    (is (= (t/eval "<%= x %>" {:x large-string})
           (apply str (repeat 100 "&lt;&gt;&amp;&quot;&#x27;")))))
  ;; Test with mixed content
  (let [mixed-content "<div>Hello & \"Welcome\" to <b>Comb</b>!</div>"]
    (is (= (t/eval "<%= x %>" {:x mixed-content})
           "&lt;div&gt;Hello &amp; &quot;Welcome&quot; to &lt;b&gt;Comb&lt;/b&gt;!&lt;/div&gt;"))))

(deftest html-escape-function-test
  (is (= (comb.template/html-escape "<script>") "&lt;script&gt;"))
  (is (= (comb.template/html-escape "Hello & World") "Hello &amp; World"))
  (is (= (comb.template/html-escape "a\"b'c") "a&quot;b&#x27;c"))
  (is (= (comb.template/html-escape nil) nil))
  (is (= (comb.template/html-escape "") ""))
  (is (= (comb.template/html-escape 123) "123")))
