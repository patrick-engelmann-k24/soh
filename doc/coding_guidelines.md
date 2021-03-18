# Coding Guidlines

We have agreed to use the following coding guidelines:
* Using Autowired constructors instead of Autowired fields, as it makes the dependencies clearly visible and allows
  easier mocking.
* Using var if the type of the variable is obvious from its assigned value, e.g. var s = "Hello world".
* Mark variables that are not modified as final.