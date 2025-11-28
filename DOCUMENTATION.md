# Comb - Web Templating Library

A powerful and intuitive Clojure templating library designed for web applications. Comb provides a familiar ERB-like syntax that makes it easy to embed Clojure code in your HTML templates, making it an excellent alternative to Selmer or Hiccup.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Template Syntax](#template-syntax)
  - [Output Expressions](#output-expressions)
  - [Code Blocks](#code-blocks)
  - [Comments](#comments)
- [Core Functions](#core-functions)
  - [template/eval](#templateeval)
  - [template/fn](#templatefn)
- [Security Considerations](#security-considerations)
  - [HTML Escaping](#html-escaping)
  - [XSS Prevention](#xss-prevention)
  - [Input Validation](#input-validation)
  - [Content Security Policy](#content-security-policy)
- [Web App Examples](#web-app-examples)
  - [HTML Pages](#html-pages)
  - [Dynamic Lists](#dynamic-lists)
  - [Conditional Rendering](#conditional-rendering)
  - [Forms and User Input](#forms-and-user-input)
  - [Layouts and Partials](#layouts-and-partials)
- [Web Framework Integration](#web-framework-integration)
  - [Ring Middleware](#ring-middleware)
  - [Reitit Integration](#reitit-integration)
  - [Component Integration](#component-integration)
- [Advanced Features](#advanced-features)
  - [Template Composition](#template-composition)
  - [Performance Optimization](#performance-optimization)
  - [Error Handling & Debugging](#error-handling--debugging)
- [Project Structure](#project-structure)
  - [Template Organization](#template-organization)
  - [Namespace Conventions](#namespace-conventions)
  - [Build Integration](#build-integration)
- [Testing Templates](#testing-templates)
  - [Unit Testing](#unit-testing)
  - [Integration Testing](#integration-testing)
  - [Performance Testing](#performance-testing)
- [Migration Guide](#migration-guide)
  - [From Selmer](#from-selmer)
  - [From Hiccup](#from-hiccup)
  - [Common Conversion Patterns](#common-conversion-patterns)
- [Development Workflow](#development-workflow)
  - [Editor Configuration](#editor-configuration)
  - [REPL Development](#repl-development)
  - [Hot Reloading](#hot-reloading)
- [Troubleshooting](#troubleshooting)
  - [Common Issues](#common-issues)
  - [Performance Problems](#performance-problems)
  - [Compatibility Concerns](#compatibility-concerns)
- [Best Practices](#best-practices)
- [Comparison with Other Libraries](#comparison-with-other-libraries)

## Installation

Add Comb to your project dependencies:

**deps.edn:**
```clojure
{:deps {comb/comb {:mvn/version "1.0.0"}}}
```

**Leiningen (project.clj):**
```clojure
[comb "1.0.0"]
```

## Quick Start

Here's a simple example to get you started:

```clojure
(require '[comb.template :as template])

;; Basic template with variable substitution
(template/eval "Hello <%= name %>!" {:name "Alice"})
;; => "Hello Alice!"

;; Template with control flow
(template/eval "
<ul>
<% (doseq [item items] %>
  <li><%= item %></li>
<% ) %>
</ul>" {:items ["Apple" "Banana" "Cherry"]})
;; => "\n<ul>\n  <li>Apple</li>\n  <li>Banana</li>\n  <li>Cherry</li>\n</ul>"
```

## Template Syntax

Comb uses two main tag types that will be familiar to anyone who has used ERB, JSP, or similar templating systems.

### Output Expressions

Use `<%= expression %>` to output the result of a Clojure expression directly into your template.

```clojure
;; Simple variable output
(template/eval "Welcome, <%= user.name %>!" {:user {:name "John"}})
;; => "Welcome, John!"

;; Function calls
(template/eval "Total: $<%= (format "%.2f" price) %>" {:price 29.99})
;; => "Total: $29.99"

;; Complex expressions
(template/eval "Items: <%= (count items) %>" {:items ["a" "b" "c"]})
;; => "Items: 3"
```

### Code Blocks

Use `<% code %>` to execute Clojure code without outputting anything. This is perfect for control structures, loops, and variable assignments.

```clojure
;; Loops
(template/eval "
<% (dotimes [i 3] %>
  <div>Item <%= i %></div>
<% ) %>")
;; => "\n  <div>Item 0</div>\n  <div>Item 1</div>\n  <div>Item 2</div>\n"

;; Conditionals
(template/eval "
<% (if user.admin? %>
  <admin-panel>Admin Content</admin-panel>
<% else %>
  <user-panel>User Content</user-panel>
<% ) %>" {:user {:admin? true}})
;; => "\n  <admin-panel>Admin Content</admin-panel>\n"

;; Variable assignments
(template/eval "
<% (def total (+ price tax)) %>
<p>Total: $<%= total %></p>" {:price 100 :tax 8})
;; => "\n<p>Total: $108</p>\n"
```

### Comments

While Comb doesn't have built-in comment syntax, you can use Clojure's comment forms:

```clojure
;; Using Clojure's comment macro
(template/eval "
<% (comment "This is a template comment") %>
<p>Visible content</p>")
;; => "\n<p>Visible content</p>\n"

;; Or just use HTML comments
(template/eval "<!-- This is an HTML comment -->")
;; => "<!-- This is an HTML comment -->"
```

## Core Functions

### template/eval

Evaluates a template string with optional bindings. This is the most straightforward way to render templates.

**Function Signature:**
```clojure
(template/eval source)
(template/eval source bindings)
```

**Parameters:**
- `source`: String or I/O source (File, Reader, InputStream) containing the template
- `bindings`: Map of variables to bind in the template (optional)

**Returns:** String containing the rendered template

```clojure
;; Basic usage without bindings
(template/eval "Static content")
;; => "Static content"

;; With bindings
(template/eval "Hello <%= name %>" {:name "World"})
;; => "Hello World"

;; Complex bindings
(template/eval "
<h1><%= post.title %></h1>
<p>By <%= post.author %> on <%= post.date %></p>
<div><%= post.content %></div>" 
{:post {:title "My Post" 
        :author "Jane Doe" 
        :date "2023-01-01"
        :content "This is the post content..."}})
```

The template source can be a string or any I/O source that `slurp` can handle:

```clojure
;; From a file
(template/eval (java.io.File. "templates/header.html") {:title "My Page"})

;; From a resource
(template/eval (clojure.java.io/resource "templates/footer.html"))

;; From a string (most common)
(template/eval "<%= message %>" {:message "Hello"})
```

**Edge Cases:**
- Empty template returns empty string
- Nil source throws exception
- Invalid Clojure syntax in template causes compilation error

### template/fn

Compiles a template into a reusable function. This is much more efficient for templates that will be rendered multiple times, as the template is only parsed once.

**Function Signature:**
```clojure
(template/fn args source)
```

**Parameters:**
- `args`: Vector of argument names for the generated function
- `source`: String or I/O source containing the template

**Returns:** Function that takes the specified arguments and returns rendered string

```clojure
;; Create a template function
(def user-card (template/fn [user] "
<div class='user-card'>
  <h3><%= user.name %></h3>
  <p>Email: <%= user.email %></p>
  <p>Role: <%= user.role %></p>
</div>"))

;; Use it multiple times efficiently
(user-card {:name "Alice" :email "alice@example.com" :role "Admin"})
;; => "\n<div class='user-card'>\n  <h3>Alice</h3>\n  <p>Email: alice@example.com</p>\n  <p>Role: Admin</p>\n</div>"

(user-card {:name "Bob" :email "bob@example.com" :role "User"})
;; => "\n<div class='user-card'>\n  <h3>Bob</h3>\n  <p>Email: bob@example.com</p>\n  <p>Role: User</p>\n</div>"
```

**Performance Note:** Template compilation happens once at function creation time, making subsequent calls much faster than repeated `template/eval` calls.

**Advanced Usage:**
```clojure
;; Multiple arguments
(def product-list (template/fn [products title] "
<h2><%= title %></h2>
<% (doseq [product products] %>
  <div><%= product.name %> - $<%= product.price %></div>
<% ) %>"))

;; Using with default values
(defn render-products [products & [title]]
  ((template/fn [products title] "...") products (or title "Products")))
```

The template source can be a string or any I/O source that `slurp` can handle:

```clojure
;; From a file
(template/eval (java.io.File. "templates/header.html") {:title "My Page"})

;; From a resource
(template/eval (clojure.java.io/resource "templates/footer.html"))

;; From a string (most common)
(template/eval "<%= message %>" {:message "Hello"})
```

### template/fn

Compiles a template into a reusable function. This is much more efficient for templates that will be rendered multiple times, as the template is only parsed once.

```clojure
;; Create a template function
(def user-card (template/fn [user] "
<div class='user-card'>
  <h3><%= user.name %></h3>
  <p>Email: <%= user.email %></p>
  <p>Role: <%= user.role %></p>
</div>"))

;; Use it multiple times efficiently
(user-card {:name "Alice" :email "alice@example.com" :role "Admin"})
;; => "\n<div class='user-card'>\n  <h3>Alice</h3>\n  <p>Email: alice@example.com</p>\n  <p>Role: Admin</p>\n</div>"

(user-card {:name "Bob" :email "bob@example.com" :role "User"})
;; => "\n<div class='user-card'>\n  <h3>Bob</h3>\n  <p>Email: bob@example.com</p>\n  <p>Role: User</p>\n</div>"
```

## Security Considerations

When using Comb in web applications, security is paramount. Since Comb allows arbitrary Clojure code execution, you must be careful about template security and output escaping.

### HTML Escaping

Comb does not automatically escape HTML output. You must manually escape user-provided content to prevent XSS attacks:

```clojure
;; Simple HTML escaping utility
(defn escape-html [s]
  (when s
    (-> s
        (.replace "&" "&amp;")
        (.replace "<" "&lt;")
        (.replace ">" "&gt;")
        (.replace "\"" "&quot;")
        (.replace "'" "&#x27;"))))

;; Safe template usage
(def safe-template (template/fn [user] "
<div class='user-profile'>
  <h1><%= (escape-html user.name) %></h1>
  <p>Bio: <%= (escape-html user.bio) %></p>
</div>"))

;; For frequently used escaping, create a helper
(def h escape-html)  ; Short alias for templates

(def user-template (template/fn [user] "
<h1><%= (h user.name) %></h1>
<p><%= (h user.comment) %></p>"))
```

### XSS Prevention

Never trust user input in templates:

```clojure
;; DANGEROUS - allows XSS
(template/eval "<%= user.comment %>" {:user {:comment "<script>alert('xss')</script>"}})

;; SAFE - escaped output
(template/eval "<%= (escape-html user.comment) %>" {:user {:comment "<script>alert('xss')</script>"}})
```

### Input Validation

Validate data before passing to templates:

```clojure
(defn validate-user [user]
  {:name (when (string? (:name user)) (subs (:name user) 0 100))
   :email (when (re-matches #".+@.+\..+" (:email user)) (:email user))
   :age (when (and (number? (:age user)) (pos? (:age user))) (:age user))})

(defn render-user-profile [raw-user]
  (let [safe-user (validate-user raw-user)]
    (user-profile-template safe-user)))
```

### Content Security Policy

Use CSP headers as additional protection:

```clojure
;; Ring middleware for CSP
(defn wrap-csp [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Content-Security-Policy"] 
                "default-src 'self'; script-src 'self'; style-src 'self'"))))

;; Usage in your Ring app
(def app (-> your-routes
             (wrap-csp)
             (wrap-params)))
```

## Web App Examples

### HTML Pages

Here's how you might structure a complete HTML page:

```clojure
(def page-template (template/fn [data] "
<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title><%= data.title %> - My Web App</title>
    <link rel='stylesheet' href='/css/style.css'>
</head>
<body>
    <header>
        <h1><%= data.site-name %></h1>
        <nav>
            <a href='/'>Home</a>
            <a href='/about'>About</a>
            <a href='/contact'>Contact</a>
        </nav>
    </header>
    
    <main>
        <%= data.content %>
    </main>
    
    <footer>
        <p>&copy; <%= data.year %> <%= data.site-name %>. All rights reserved.</p>
    </footer>
</body>
</html>"))

;; Usage
(page-template {
  :title "Welcome"
  :site-name "MyApp"
  :year "2023"
  :content "<h2>Welcome to our site!</h2><p>This is the home page.</p>"
})
```

### Dynamic Lists

Perfect for rendering lists of items like blog posts, products, or menu items:

```clojure
(def blog-list (template/fn [posts] "
<div class='blog-list'>
  <h2>Recent Posts</h2>
  <% (if (empty? posts) %>
    <p>No posts available.</p>
  <% else %>
    <% (doseq [post posts] %>
      <article class='blog-post'>
        <h3><a href='/posts/<%= post.id %>'><%= post.title %></a></h3>
        <p class='meta'>
          By <%= post.author %> on <%= post.date %>
          <% (when post.tags %>
            | Tags: 
            <% (doseq [tag post.tags] %>
              <span class='tag'><%= tag %></span>
            <% ) %>
          <% ) %>
        </p>
        <div class='excerpt'>
          <%= post.excerpt %>
        </div>
        <a href='/posts/<%= post.id %>' class='read-more'>Read more →</a>
      </article>
    <% ) %>
  <% ) %>
</div>"))

;; Usage
(blog-list [
  {:id "1" :title "First Post" :author "John" :date "2023-01-01" 
   :excerpt "This is the first post..." :tags ["clojure" "web"]}
  {:id "2" :title "Second Post" :author "Jane" :date "2023-01-02" 
   :excerpt "Another interesting post..." :tags ["programming"]}
])
```

### Conditional Rendering

Show different content based on user roles, data availability, or other conditions:

```clojure
(def user-dashboard (template/fn [user] "
<div class='dashboard'>
  <h2>Welcome back, <%= user.name %>!</h2>
  
  <% (if (= user.role :admin) %>
    <div class='admin-panel'>
      <h3>Admin Tools</h3>
      <button onclick='manageUsers()'>Manage Users</button>
      <button onclick='viewAnalytics()'>View Analytics</button>
      <button onclick='systemSettings()'>System Settings</button>
    </div>
  <% else %>
    <div class='user-panel'>
      <h3>Your Account</h3>
      <p>Email: <%= user.email %></p>
      <p>Member since: <%= user.join-date %></p>
      <button onclick='editProfile()'>Edit Profile</button>
    </div>
  <% ) %>
  
  <% (when user.premium? %>
    <div class='premium-features'>
      <h3>Premium Features</h3>
      <p>Thank you for being a premium member!</p>
      <a href='/premium-content'>Access Premium Content</a>
    </div>
  <% ) %>
  
  <% (when (seq user.notifications) %>
    <div class='notifications'>
      <h3>Notifications</h3>
      <% (doseq [notif user.notifications] %>
        <div class='notification <%= notif.type %>'>
          <%= notif.message %>
          <small><%= notif.timestamp %></small>
        </div>
      <% ) %>
    </div>
  <% ) %>
</div>"))
```

### Forms and User Input

Create dynamic forms with validation states and pre-filled values:

```clojure
(def user-form (template/fn [user errors] "
<form class='user-form' method='POST' action='/users/save'>
  <div class='form-group'>
    <label for='name'>Name:</label>
    <input type='text' id='name' name='name' 
           value='<%= (:name user "") %>' 
           class='<%= (when (:name errors) "error") %>'>
    <% (when (:name errors) %>
      <span class='error-message'><%= (:name errors) %></span>
    <% ) %>
  </div>
  
  <div class='form-group'>
    <label for='email'>Email:</label>
    <input type='email' id='email' name='email' 
           value='<%= (:email user "") %>'
           class='<%= (when (:email errors) "error") %>'>
    <% (when (:email errors) %>
      <span class='error-message'><%= (:email errors) %></span>
    <% ) %>
  </div>
  
  <div class='form-group'>
    <label for='role'>Role:</label>
    <select id='role' name='role'>
      <option value='user' <%= (when (= (:role user) "user") "selected") %>>User</option>
      <option value='admin' <%= (when (= (:role user) "admin") "selected") %>>Admin</option>
      <option value='moderator' <%= (when (= (:role user) "moderator") "selected") %>>Moderator</option>
    </select>
  </div>
  
  <div class='form-group'>
    <label>
      <input type='checkbox' name='active' <%= (when (:active user) "checked") %>>
      Active User
    </label>
  </div>
  
  <div class='form-actions'>
    <button type='submit'>Save User</button>
    <a href='/users' class='cancel'>Cancel</a>
  </div>
</form>"))
```

### Layouts and Partials

Create reusable layouts and partial templates:

```clojure
;; Layout template
(def layout (template/fn [title content] "
<!DOCTYPE html>
<html>
<head>
    <title><%= title %> - MyApp</title>
    <link rel='stylesheet' href='/css/app.css'>
</head>
<body>
    <header>
        <nav>
            <a href='/'>Home</a>
            <a href='/about'>About</a>
            <a href='/contact'>Contact</a>
        </nav>
    </header>
    
    <main>
        <%= content %>
    </main>
    
    <footer>
        <p>&copy; 2023 MyApp</p>
    </footer>
</body>
</html>"))

;; Partial templates
(def navigation (template/fn [current-page] "
<nav class='main-nav'>
    <a href='/' class='<%= (when (= current-page "home") "active") %>'>Home</a>
    <a href='/products' class='<%= (when (= current-page "products") "active") %>'>Products</a>
    <a href='/about' class='<%= (when (= current-page "about") "active") %>'>About</a>
    <a href='/contact' class='<%= (when (= current-page "contact") "active") %>'>Contact</a>
</nav>"))

(def product-card (template/fn [product] "
<div class='product-card'>
    <img src='<%= product.image %>' alt='<%= product.name %>'>
    <h3><%= product.name %></h3>
    <p class='price'>$<%= product.price %></p>
    <p class='description'><%= product.description %></p>
    <% (when product.on-sale? %>
        <span class='sale-badge'>On Sale!</span>
    <% ) %>
    <button onclick='addToCart(\"<%= product.id %>\")'>Add to Cart</button>
</div>"))

;; Using them together
(defn render-product-page [products]
  (layout "Products" (str "
    <h2>Our Products</h2>
    <div class='products-grid'>
      " (apply str (map #(product-card %) products)) "
    </div>
  ")))
```

## Web Framework Integration

### Ring Middleware

Create Ring middleware for template rendering:

```clojure
(require '[ring.middleware.params :refer [wrap-params]]
         '[ring.middleware.keyword-params :refer [wrap-keyword-params]])

(defn wrap-template [handler template-fn]
  (fn [request]
    (let [response (handler request)
          template-data (merge (:template-data response) 
                               {:request request
                                :session (:session request)})]
      (if (:template response)
        (assoc response :body (template-fn template-data))
        response))))

;; Usage
(defn home-handler [request]
  {:template "home"
   :template-data {:user (:user request)
                   :message "Welcome!"}})

(def app (-> (routes home-handler)
             (wrap-template home-template)
             (wrap-keyword-params)
             (wrap-params)))
```

### Reitit Integration

Integrate with Reitit routing:

```clojure
(require '[reitit.ring :as ring]
         '[reitit.ring.middleware :as middleware])

(defn render-template [template-name data]
  (case template-name
    "users" (users-list-template data)
    "user-detail" (user-detail-template data)
    "404" (not-found-template data)))

(defn template-handler [template-name data-fn]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (render-template template-name (data-fn request))}))

(def routes
  (ring/router
    ["/" {:middleware [middleware/parameters
                      middleware/session]}
     ["/users" {:get (template-handler "users" 
                                       (fn [req] {:users (get-all-users)}))}]
     ["/users/:id" {:get (template-handler "user-detail"
                                           (fn [req] {:user (get-user-by-id (get-in req [:path-params :id]))}))}]))
```

### Component Integration

Integrate with Component or similar systems:

```clojure
(defrecord TemplateRenderer [templates]
  component/Lifecycle
  (start [this]
    (let [compiled-templates 
          (into {} 
                (for [[name template] templates]
                  [name (template/fn [data] template)]))]
      (assoc this :compiled compiled-templates)))
  (stop [this]
    (assoc this :compiled nil))
  
  (render [this template-name data]
    (if-let [template-fn (get-in this [:compiled template-name])]
      (template-fn data)
      (throw (ex-info "Template not found" {:template template-name})))))

;; Usage
(def renderer (component/start 
                (->TemplateRenderer 
                  {"home" "<h1>Welcome <%= user.name %></h1>"
                   "about" "<h2>About Us</h2>"})))

(renderer/render "home" {:user {:name "Alice"}})
```

## Advanced Features

### Template Composition

You can build complex templates by combining simpler ones:

```clojure
(def table-row (template/fn [row] "
<tr>
  <% (doseq [cell row] %>
    <td><%= cell %></td>
  <% ) %>
</tr>"))

(def data-table (template/fn [headers rows] "
<table class='data-table'>
  <thead>
    <tr>
      <% (doseq [header headers] %>
        <th><%= header %></th>
      <% ) %>
    </tr>
  </thead>
  <tbody>
    <% (doseq [row rows] %>
      " (table-row row) "
    <% ) %>
  </tbody>
</table>"))

;; Usage
(data-table 
  ["Name" "Age" "City"] 
  [["Alice" "25" "New York"] ["Bob" "30" "San Francisco"]])
```

### Performance Optimization

Use `template/fn` for templates that will be rendered multiple times:

```clojure
;; Bad - parses template every time
(defn render-user-list-slow [users]
  (template/eval "
    <ul>
      <% (doseq [user users] %>
        <li><%= user.name %> - <%= user.email %></li>
      <% ) %>
    </ul>" {:users users}))

;; Good - compiles once, reuses many times
(def user-list-template (template/fn [users] "
  <ul>
    <% (doseq [user users] %>
      <li><%= user.name %> - <%= user.email %></li>
    <% ) %>
  </ul>"))

(defn render-user-list-fast [users]
  (user-list-template users))
```

**Performance Benchmarks:**
```clojure
;; Benchmarking template rendering
(defn benchmark-template []
  (let [users (repeatedly 1000 #(hash-map :name (str "user" %) :email (str "user" % "@example.com")))]
    (time 
      (dotimes [_ 1000]
        (render-user-list-slow users)))
    (time
      (dotimes [_ 1000]
        (render-user-list-fast users)))))
;; template/eval: ~2000ms
;; template/fn: ~50ms (40x faster)
```

### Error Handling & Debugging

Handle template errors gracefully:

```clojure
(defn safe-template-eval [template data]
  (try
    (template/eval template data)
    (catch Exception e
      (log/error "Template error" e)
      "<!-- Template error -->")))

;; Debug template compilation
(defn debug-template-fn [args source]
  (try
    (template/fn args source)
    (catch Exception e
      (println "Template compilation failed:")
      (println "Source:" source)
      (println "Error:" (.getMessage e))
      (throw e))))

;; Template validation
(defn validate-template [source]
  (try
    (template/fn [] source)
    true
    (catch Exception e
      false)))
```

## Project Structure

### Template Organization

Organize templates in a logical directory structure:

```
resources/
├── templates/
│   ├── layouts/
│   │   ├── base.html
│   │   └── admin.html
│   ├── partials/
│   │   ├── header.html
│   │   ├── footer.html
│   │   └── navigation.html
│   ├── pages/
│   │   ├── home.html
│   │   ├── about.html
│   │   └── contact.html
│   └── emails/
│       ├── welcome.html
│       └── password-reset.html
```

### Namespace Conventions

Follow consistent naming patterns:

```clojure
;; Template compilation namespace
(ns myapp.templates.core
  (:require [comb.template :as template]
            [myapp.templates.layouts :as layouts]
            [myapp.templates.pages :as pages]))

;; Layout templates
(ns myapp.templates.layouts
  (:require [comb.template :as template]))

(def base (template/fn [title content] "..."))
(def admin (template/fn [title content] "..."))

;; Page templates
(ns myapp.templates.pages
  (:require [comb.template :as template]
            [myapp.templates.partials :as partials]))

(def home (template/fn [data] "..."))
(def about (template/fn [data] "..."))
```

### Build Integration

Integrate with Leiningen or tools.build:

```clojure
;; project.clj
(defproject myapp "0.1.0"
  :profiles {:dev {:resource-paths ["dev-resources"]}}
  :prep-tasks [["compile" "myapp.templates"]])

;; tools.build (build.clj)
(ns build
  (:require [clojure.tools.build.api :as b]
            [comb.template :as template]))

(defn compile-templates [opts]
  (let [template-dir "resources/templates"
        output-dir "target/classes/templates"]
    (b/delete {:path output-dir})
    (b/write-file {:path (str output-dir "/compiled.edn")
                   :content (pr-str (compile-all-templates template-dir)})))
```

## Testing Templates

### Unit Testing

Test template functions directly:

```clojure
(ns myapp.templates-test
  (:require [clojure.test :refer [deftest is testing]]
            [myapp.templates :as templates]))

(deftest user-card-template-test
  (testing "renders user card correctly"
    (let [user {:name "Alice" :email "alice@example.com" :role "admin"}
          result (templates/user-card user)]
      (is (.contains result "Alice"))
      (is (.contains result "alice@example.com"))
      (is (.contains result "admin"))))
  
  (testing "handles missing data gracefully"
    (let [user {:name nil :email "" :role nil}
          result (templates/user-card user)]
      (is (.contains result "<h3></h3>")))))

(deftest template-security-test
  (testing "escapes HTML properly"
    (let [malicious "<script>alert('xss')</script>"
          result (templates/safe-output malicious)]
      (is (not (.contains result "<script>")))
      (is (.contains result "&lt;script&gt;")))))
```

### Integration Testing

Test templates in web context:

```clojure
(ns myapp.web-test
  (:require [clojure.test :refer [deftest is]]
            [ring.mock.request :as mock]
            [myapp.web :as web]))

(deftest home-page-template-test
  (let [response (web/app (mock/request :get "/"))
        body (:body response)]
    (is (= 200 (:status response)))
    (is (.contains body "Welcome"))
    (is (.contains body "<!DOCTYPE html>"))))

(deftest user-profile-template-test
  (let [response (web/app (mock/request :get "/users/123"))
        body (:body response)]
    (is (= 200 (:status response)))
    (is (.contains body "User Profile"))
    (is (.contains body "user123@example.com"))))
```

### Performance Testing

Benchmark template performance:

```clojure
(ns myapp.performance-test
  (:require [criterium.core :as criterium]
            [myapp.templates :as templates]))

(deftest template-performance-test
  (testing "user-list template performance"
    (let [users (repeatedly 1000 #(hash-map :name (str "user" %) :email (str "user" % "@example.com")))]
      (criterium/bench 
        (templates/user-list users)
        :warmup-jit-period 10
        :samples 100))))
```

## Migration Guide

### From Selmer

Convert Selmer syntax to Comb:

```clojure
;; Selmer syntax
"{{ user.name }}"
"{% if user.admin %}"
"{% for item in items %}"
"{{ item|upper }}"

;; Comb equivalent
"<%= user.name %>"
"<% (if user.admin %>"
"<% (doseq [item items] %>"
"<%= (clojure.string/upper-case item) %>"
```

**Migration Strategy:**
```clojure
;; Gradual migration - create compatibility layer
(defn selmer->comb [template]
  (-> template
      (.replace "{{ " "<%= ")
      (.replace " }}" " %>")
      (.replace "{% if " "<% (if ")
      (.replace " %}" " %>")
      (.replace "{% for " "<% (doseq [")
      (.replace " in " " ")
      (.replace " %}" " %>")
      (.replace "{{ " "<%= ")
      (.replace " }}" " %>")
      (.replace "|upper" ") (clojure.string/upper-case ")
      (.replace "|date:" ") (java.text.SimpleDateFormat. ")
      (.replace "|default:" ") (or ")))
```

### From Hiccup

Convert Hiccup data structures to Comb templates:

```clojure
;; Hiccup
[:div {:class "user-card"}
 [:h3 (:name user)]
 [:p (:email user)]]

;; Comb equivalent
"<div class='user-card'>
  <h3><%= user.name %></h3>
  <p><%= user.email %></p>
</div>"
```

**Hiccup to Comb converter:**
```clojure
(defn hiccup->comb [element]
  (cond
    (string? element) element
    (vector? element) (let [[tag attrs & children] element
                           attrs-str (if (map? attrs)
                                      (str " " (->> attrs
                                                   (map (fn [[k v]] (str (name k) "='" v "'")))
                                                   (clojure.string/join " ")))
                                      "")]
                       (str "<" (name tag) attrs-str ">"
                            (apply str (map hiccup->comb children))
                            "</" (name tag) ">"))
    :else (str element)))
```

### Common Conversion Patterns

```clojure
;; Conditional rendering
;; Selmer: {% if user.admin %}Admin content{% endif %}
;; Comb: <% (if user.admin %>Admin content<% ) %>

;; Loops
;; Selmer: {% for item in items %}{{ item }}{% endfor %}
;; Comb: <% (doseq [item items] %><%= item %><% ) %>

;; Filters
;; Selmer: {{ user.name|upper|default:"Anonymous" }}
;; Comb: <%= (clojure.string/upper-case (or user.name "Anonymous")) %>

;; Includes
;; Selmer: {% include "header.html" %}
;; Comb: <%= (header-template data) %>
```

## Development Workflow

### Editor Configuration

Configure your editor for Comb templates:

**VS Code settings.json:**
```json
{
  "files.associations": {
    "*.html": "html",
    "*.comb": "clojure"
  },
  "emmet.includeLanguages": {
    "clojure": "html"
  }
}
```

**Emacs configuration:**
```elisp
(add-to-list 'auto-mode-alist '("\\.comb\\'" . clojure-mode))
(add-hook 'clojure-mode-hook
          (lambda ()
            (when (string-suffix-p ".comb" buffer-file-name)
              (setq-local indent-tabs-mode nil)
              (setq-local clojure-indent-style :always-align))))
```

### REPL Development

Develop templates interactively:

```clojure
;; Start REPL with template reloading
(require '[myapp.templates :as templates])
(require '[clojure.java.io :as io])

;; Reload template from file
(defn reload-template [name]
  (let [template-path (str "resources/templates/" name ".html")]
    (when (.exists (io/file template-path))
      (alter-var-root (resolve (symbol "myapp.templates" (str name "-template")))
                      (constantly (template/fn [data] (slurp template-path)))))))

;; Interactive template testing
(comment
  (reload-template "user-card")
  (templates/user-card {:name "Test" :email "test@example.com"})
  )
```

### Hot Reloading

Implement template hot reloading in development:

```clojure
(defn wrap-template-reload [handler]
  (fn [request]
    (when (= :dev (:env request))
      (reload-all-templates))
    (handler request)))

;; File watcher for development
(defn watch-templates []
  (future
    (while true
      (Thread/sleep 1000)
      (when (templates-changed?)
        (reload-all-templates)
        (println "Templates reloaded")))))
```

## Troubleshooting

### Common Issues

**Template not found:**
```clojure
;; Problem: Template file path incorrect
(template/eval "templates/header.html" data)  ; File not found

;; Solution: Use absolute paths or resources
(template/eval (io/resource "templates/header.html") data)
```

**Variable not bound:**
```clojure
;; Problem: Variable name mismatch
(template/eval "<%= username %>" {:user-name "Alice"})  ; username not bound

;; Solution: Match variable names exactly
(template/eval "<%= user-name %>" {:user-name "Alice"})
```

**Syntax errors in templates:**
```clojure
;; Problem: Invalid Clojure syntax
(template/eval "<%= (if user %>Hello<% ) %>" {:user true})  ; Missing closing paren

;; Solution: Validate Clojure syntax
(defn validate-template-syntax [template]
  (try
    (read-string (str "(" template ")"))
    true
    (catch Exception e
      (println "Syntax error:" (.getMessage e))
      false)))
```

### Performance Problems

**Slow template rendering:**
```clojure
;; Problem: Using template/eval in loops
(defn render-users-slow [users]
  (map #(template/eval "<li><%= %></li>" {:user %}) users))

;; Solution: Pre-compile templates
(def user-item-template (template/fn [user] "<li><%= user.name %></li>"))
(defn render-users-fast [users]
  (map user-item-template users))
```

**Memory leaks:**
```clojure
;; Problem: Creating many template functions
(defn create-template-factory []
  (fn [data] (template/fn [data] "<%= data %>")))  ; New function each call

;; Solution: Cache compiled templates
(def template-cache (atom {}))
(defn get-cached-template [source]
  (or (@template-cache source)
      (let [compiled (template/fn [data] source)]
        (swap! template-cache assoc source compiled)
        compiled)))
```

### Compatibility Concerns

**Clojure version compatibility:**
```clojure
;; Check Clojure version
(when (< (:major *clojure-version*) 1)
  (throw (ex-info "Comb requires Clojure 1.0+" {})))
```

**Java version issues:**
```clojure
;; Ensure Java 8+ for modern web apps
(when (< (Integer/parseInt (re-find #"\d+" (System/getProperty "java.version"))) 8)
  (println "Warning: Java 8+ recommended for web applications"))
```

## Best Practices

1. **Use `template/fn` for reusable templates** - Compile once, use many times
2. **Always escape user input** - Prevent XSS attacks with HTML escaping
3. **Keep templates simple** - Move complex logic to your Clojure code
4. **Use meaningful variable names** - Makes templates more readable
5. **Organize templates by feature** - Group related templates together
6. **Use partials for common elements** - Headers, footers, navigation, etc.
7. **Validate template syntax** - Check templates during development
8. **Implement caching** - Cache compiled templates in production
9. **Test templates thoroughly** - Unit test, integration test, and performance test
10. **Use version control** - Track template changes alongside code
11. **Document template contracts** - Specify expected data structures
12. **Monitor performance** - Benchmark template rendering in production

## Comparison with Other Libraries

### vs Selmer
- **Comb**: ERB-like syntax, full Clojure expressions, more flexible
- **Selmer**: Django-like syntax, sandboxed, more restrictive but safer
- **Performance**: Comb is generally faster due to direct Clojure compilation
- **Learning Curve**: Comb easier for developers with ERB/JSP experience

### vs Hiccup
- **Comb**: Template files separate from code, easier for designers to edit
- **Hiccup**: Clojure data structures, more programmatic but less designer-friendly
- **Type Safety**: Hiccup has better compile-time checking
- **Tooling**: Comb works better with existing HTML tooling

### vs Enlive
- **Comb**: Simpler, more intuitive for basic templating, easier to learn
- **Enlive**: More powerful transformation capabilities, functional approach
- **Performance**: Comb generally faster for simple rendering
- **Use Cases**: Comb for content generation, Enlive for HTML transformation

### vs Mustache/Handlebars
- **Comb**: Full programming language support, more powerful
- **Mustache**: Logic-less templates, more restrictive but simpler
- **Security**: Mustache inherently safer due to logic-less nature
- **Flexibility**: Comb can handle any rendering logic, Mustache limited

Comb strikes a balance between simplicity and power, making it ideal for most web applications where you need to render dynamic HTML content without the complexity of more heavyweight templating systems. It's particularly well-suited for Clojure applications where developers want full access to the language within templates.