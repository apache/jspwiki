```
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
```

# 1. Introduction

## 1.1 What Is Inside?

This project builds a ready-to-use JSP Wiki distribution that is configured to use 
container (tomcat, jboss, etc) based user authentication and authorization rules.

This also uses the "Advanced" ACL and Authorization managers, which enables
boolean logic for page level access control.

* Based on embedded Tomcat servlet engine with minimal memory foot-print 
* Using HTTP port 9627 to avoid conflicts with existing servers running on port 80 and/or 8080

