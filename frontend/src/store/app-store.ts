/*
 * Copyright (c) 2023 Jean Schmitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {defineStore} from 'pinia';
import ErrorMessage from '@/store/to/error-message';

export const useAppStore = defineStore('app', {
  state: () => {
    return {
      errors: [] as Array<ErrorMessage>,
    };
  },
  actions: {
    hasErrors() {
      return this.errors.length > 0;
    },
    addError(errorMessage: string) {
      const error = new ErrorMessage();
      error.id = new Date().getTime().toString();
      error.message = errorMessage;
      this.errors.push(error);
    },
    clearErrors() {
      this.errors = [];
    },
  },
});
