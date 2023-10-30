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

import {convert} from 'html-to-text';

class HtmlToTextService {
  removeHtml(htmlContent: string): string {
    // See https://github.com/html-to-text/node-html-to-text/blob/master/packages/html-to-text/README.md
    const options = {
      decodeEntities: false,
      wordwrap: null,
      selectors: [
        //{ selector: 'a', options: { baseUrl: 'https://example.com' } },
        {selector: 'img', format: 'skip'},
      ],
    };

    return convert(htmlContent, options);
  }
}

export default HtmlToTextService;
