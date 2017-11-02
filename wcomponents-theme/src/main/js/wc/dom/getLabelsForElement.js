define(["wc/dom/tag",
	"wc/dom/Widget",
	"wc/array/toArray",
	"wc/dom/wrappedInput"],
	function(tag, Widget, toArray, wrappedInput) {
		"use strict";

		var LABELABLE = [tag.INPUT, tag.SELECT, tag.TEXTAREA, tag.PROGRESS],
			FIELDSET,
			LEGEND,
			LABEL;

		/**
		 * Get labels and/or standins suing querySelector.
		 *
		 * @function
		 * @private
		 * @param {Element} element The labelled element.
		 * @param {Element[]} [labelArr] Labels we have already found.
		 * @param {Boolean} readOnly If true also get labels for element in its read-only state.
		 * @returns {Array} If element has no 'labels' then an empty array is returned.
		 */
		function doLabelQuery(element, labelArr, readOnly) {
			var result = labelArr || [],
				id = element.id,
				wrappedId,
				query;

			if (id) {
				query = "label[for=\"" + id + "\"],[data-wc-for=\"" + id + "\"]";

				if (readOnly) {
					query += ",[data-wc-rofor=\"" + id + "\"]";
					if (wrappedInput.isReadOnly(element) && (wrappedId = wrappedInput.getWrappedId(element))) {
						// we may be in an AJAX situation where we are trying to convert
						// labels to spans or vice-versa.
						query += ",label[for=\"" + wrappedId + "\"]";
					}
				}
				result = result.concat(toArray(document.querySelectorAll(query)));
			}

			return result;
		}

		function getLabelsForWrapper(element, includeReadOnly) {
			var result = [], _input;
			if (includeReadOnly) {
				result = doLabelQuery(element, result, true);
			}
			if ((_input = wrappedInput.getInput(element))) {
				return doLabelQuery(_input, result);
			}
			return result;
		}

		function getAncestorLabel(element) {
			var label;
			LABEL = LABEL || new Widget(tag.LABEL);

			if (!(label = LABEL.findAncestor(element))) {
				return [];
			}

			if (!label.hasAttribute("for") || (label.getAttribute("for") === element.id)) {
				return [label];
			}
			return [];
		}

		/**
		 * Get <label> defined in aria-labelledby attribute
		 * @param {Element} element The element with or without aria-labelledby attribute
		 * @returns {Array} An array of <label> elements. If attribute aria-labelledby is not found 
		 * or no <label> with ID defined is found, then an empty array is returned.
		 */
		function getAriaLabels(element) {
			var labelIds, ariaLabels = [], lblElement;
			labelIds = element.getAttribute("aria-labelledby");
			if (labelIds) {
				labelIds.split(/(\s+)/).forEach(function(labelId) {
					lblElement = document.getElementById(labelId);
					if (lblElement) {
						ariaLabels.push(lblElement);
					}
				});
			}
			return ariaLabels;
		}

		/**
		 * Gets labelling element/s (label, legend or pseudo-label) for a control.
		 *
		 * @function module:wc/dom/getLabelsForElement
		 * @param {Element} element The element for which we want to find labels.
		 * @param {Boolean} [includeReadOnly] If true then also search for "labelling" elements for read only controls.
		 * @returns {Array} An array of elements which 'label' element. If element has no 'labels' then an empty array
		 *  is returned.
		 */
		function getLabels(element, includeReadOnly) {
			if (element) {
				var ariaLabels,
					nativeLabels = [],
					label,
					tagName;

				ariaLabels = getAriaLabels(element);

				if (wrappedInput.isOneOfMe(element, includeReadOnly)) {
					nativeLabels = getLabelsForWrapper(element, includeReadOnly);
				} else {
					FIELDSET = FIELDSET || new Widget(tag.FIELDSET);
					if (FIELDSET.isOneOfMe(element)) {
						LEGEND = LEGEND || new Widget("legend");
						if ((label = LEGEND.findDescendant(element, true))) {
							nativeLabels = [label];
						}
					}

					nativeLabels = doLabelQuery(element, nativeLabels, includeReadOnly);

					if (!(nativeLabels && nativeLabels.length)) {
						// try getting an ancestor label element ONLY if element is input, textarea, select or progress.
						tagName = element.tagName;
						if (LABELABLE.indexOf(tagName) > -1) {
							nativeLabels = getAncestorLabel(element);
						}
					}
				}
				// Append ariaLabel elements at the end of nativeLabel elements
				return nativeLabels.concat(ariaLabels);
			} else {
				// If element is not found return empty array
				return [];
			}
		}
		/**
		 * @module
		 * @requires module:wc/dom/tag
		 * @requires module:wc/dom/Widget
		 * @requires module:wc/array/toArray
		 * @requires module:wc/dom/wrappedInput
		 */
		return getLabels;
	});
