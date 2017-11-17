# Jsoup based Furaffinity API
This library accesses the web representation of Furaffinity to get all information needed for processing.

## Userpages
A userpage can be retireved by calling `userpage(user, FA.default)`. It contains almost all information present on the page, includign submissions, favorites, journals, artist info, shouts, as well as watches (incoming and outgoing).

## Submissions and favorites
Submissions and favorites contains all submissions and favorites respectively, with their preview image, content url, their comments, metadata etcetera. The results can be given by page (`submissionPage(user, pagenumber, FA.default)`), as a page sequence, only pulling elements when iterated (`submissionPages(user, FA.default)`) or as pure submissions in the sequence defined by pages (`submissions(user, FA.default`).

## Journals
Journals contain the metadata and content, as well as the comments. This is very similar to the calls to submissions and favorites.

## Comments
Comments are given as comments with IDs and the comment IDs they reply to. They can be preprocessed by calling `toTree()`, where comments are associated to their immediate children recursively.

# Building
`./gradlew build` should suffice to download all necessary dependencies.

# Stability
This is not a native API, it uses HTML analysis to generate the data. If the layout changes too heavily, the data might be corrupted.
