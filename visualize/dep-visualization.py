import pandas as pd
import numpy as np

import stanfordnlp
from spacy_stanfordnlp import StanfordNLPLanguage
from spacy import displacy
from pathlib import Path
import os

def create_visualize_html(input_file, df_snippets, df_3192=None):
    options = {"compact": True, "bg": "#09a3d5",
           "color": "white", "font": "Source Sans Pro"}

    count = 0
    for index, row in df_snippets.iterrows():
        is_in = False
        if input_file == 'others': # check duplicate with 3192
            for j, r in df_3192.iterrows():
                if str(r["SNIPPET"]) in str(row["SNIPPET"]):
                    is_in = True
                    break
        if is_in == False:
            if count > 20:
                break
            count += 1
            doc = nlp(str(row["SNIPPET"]))
            sentence_spans = list(doc.sents)
            html = displacy.render(sentence_spans, style="dep", page=True, options=options)

            output_path = Path("./%s/%d.html" % (input_file, row["Row ID"]))
            output_path.open("w", encoding="utf-8").write(html)

if __name__ == '__main__':
    if not os.path.exists("3192"):
        os.makedirs("3192")
        os.makedirs("others")
    snlp = stanfordnlp.Pipeline(lang="en")
    nlp = StanfordNLPLanguage(snlp)

    # from 3192 file
    df = pd.read_csv('3192antibody.tsv', sep='\t')
    create_visualize_html("3192", df)

    # from all_snippets file
    df_3192 = df[0:30]
    df_snippets = pd.read_csv('all_snippets.tsv', sep='\t')
    create_visualize_html("others", df_snippets, df_3192)